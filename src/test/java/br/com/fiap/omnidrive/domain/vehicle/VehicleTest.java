package br.com.fiap.omnidrive.domain.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.exception.InvalidStatusTransitionException;
import java.time.Year;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Testes das regras de cadastro, edição e transição de status do {@link Vehicle}.
 */
class VehicleTest {

  private static final UUID SALE_ID = UUID.randomUUID();

  static Stream<Arguments> invalidTransitions() {
    return Stream.of(
            transition("startPreparation", VehicleStatus.RECEIVED, Vehicle::startPreparation),
            transition("releaseForSale", VehicleStatus.IN_PREPARATION, Vehicle::releaseForSale),
            transition("reserve", VehicleStatus.AVAILABLE, v -> v.reserve(SALE_ID)),
            transition("confirmSale", VehicleStatus.RESERVED, v -> v.confirmSale(SALE_ID)),
            transition("releaseReservation", VehicleStatus.RESERVED, v -> v.releaseReservation(SALE_ID)),
            transition("reverseSale", VehicleStatus.SOLD, v -> v.reverseSale(SALE_ID)),
            transition("readmit", VehicleStatus.SOLD, v -> v.readmit(Money.of("35000"))))
        .flatMap(t -> t);
  }

  private static Stream<Arguments> transition(String name, VehicleStatus validStatus, Consumer<Vehicle> action) {
    return Arrays.stream(VehicleStatus.values())
        .filter(status -> status != validStatus)
        .map(status -> Arguments.of(name, status, action));
  }

  private static Vehicle register() {
    return Vehicle.register("Fiat", "Uno", 2020, "Prata", Money.of("30000"), Money.of("40000"),
        VehicleOrigin.OWNED);
  }

  private static Vehicle restore(VehicleStatus status) {
    return Vehicle.restore(UUID.randomUUID(), "Fiat", "Uno", 2020, "Prata", Money.of("30000"), Money.of("40000"),
        VehicleOrigin.OWNED, status);
  }

  @Test
  void registrationStartsAsReceivedAndRecordsHistory() {
    var vehicle = register();

    assertThat(vehicle.getId()).isNotNull();
    assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.RECEIVED);
    assertThat(vehicle.getStatusChanges())
        .singleElement()
        .satisfies(history -> {
          assertThat(history.vehicleId()).isEqualTo(vehicle.getId());
          assertThat(history.previousStatus()).isNull();
          assertThat(history.newStatus()).isEqualTo(VehicleStatus.RECEIVED);
          assertThat(history.saleId()).isNull();
          assertThat(history.occurredAt()).isNotNull();
        });
  }

  @Test
  void registrationStripsTexts() {
    var vehicle = Vehicle.register("  Fiat ", " Uno ", 2020, " Prata ", Money.of("30000"), Money.of("40000"),
        VehicleOrigin.OWNED);

    assertThat(vehicle.getBrand()).isEqualTo("Fiat");
    assertThat(vehicle.getModel()).isEqualTo("Uno");
    assertThat(vehicle.getColor()).isEqualTo("Prata");
  }

  @Test
  void goesThroughFullSaleAndReadmissionCycle() {
    var vehicle = register();

    vehicle.startPreparation();
    vehicle.releaseForSale();
    vehicle.reserve(SALE_ID);
    vehicle.confirmSale(SALE_ID);
    vehicle.readmit(Money.of("35000"));

    assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.RECEIVED);
    assertThat(vehicle.getPurchasePrice()).isEqualTo(Money.of("35000"));
    assertThat(vehicle.getStatusChanges())
        .extracting(VehicleStatusHistory::previousStatus, VehicleStatusHistory::newStatus,
            VehicleStatusHistory::saleId)
        .containsExactly(
            tuple(null, VehicleStatus.RECEIVED, null),
            tuple(VehicleStatus.RECEIVED, VehicleStatus.IN_PREPARATION, null),
            tuple(VehicleStatus.IN_PREPARATION, VehicleStatus.AVAILABLE, null),
            tuple(VehicleStatus.AVAILABLE, VehicleStatus.RESERVED, SALE_ID),
            tuple(VehicleStatus.RESERVED, VehicleStatus.SOLD, SALE_ID),
            tuple(VehicleStatus.SOLD, VehicleStatus.RECEIVED, null));
  }

  @Test
  void readmissionKeepsPreviousPurchasePriceInHistory() {
    var vehicle = restore(VehicleStatus.SOLD);

    vehicle.readmit(Money.of("35000"));

    assertThat(vehicle.getStatusChanges().getLast().reason()).contains("30000.00");
  }

  @Test
  void releasingReservationReturnsToAvailable() {
    var vehicle = restore(VehicleStatus.RESERVED);

    vehicle.releaseReservation(SALE_ID);

    assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    assertThat(vehicle.getStatusChanges().getLast().saleId()).isEqualTo(SALE_ID);
  }

  @Test
  void reversingSaleReturnsToAvailable() {
    var vehicle = restore(VehicleStatus.SOLD);

    vehicle.reverseSale(SALE_ID);

    assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    assertThat(vehicle.getStatusChanges().getLast().saleId()).isEqualTo(SALE_ID);
  }

  @Test
  void restoreDoesNotRecordHistory() {
    assertThat(restore(VehicleStatus.AVAILABLE).getStatusChanges()).isEmpty();
  }

  @ParameterizedTest(name = "{0} from {1}")
  @MethodSource("invalidTransitions")
  void rejectsTransitionFromInvalidStatus(String transition, VehicleStatus status, Consumer<Vehicle> action) {
    var vehicle = restore(status);

    assertThatThrownBy(() -> action.accept(vehicle)).isInstanceOf(InvalidStatusTransitionException.class);
    assertThat(vehicle.getStatus()).isEqualTo(status);
    assertThat(vehicle.getStatusChanges()).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(names = {"RECEIVED", "IN_PREPARATION", "AVAILABLE"})
  void updatesInEditableStatuses(VehicleStatus status) {
    var vehicle = restore(status);

    vehicle.update("Volkswagen", "Gol", 2019, "Branco", Money.of("25000"), Money.of("32000"),
        VehicleOrigin.CONSIGNED);

    assertThat(vehicle.getBrand()).isEqualTo("Volkswagen");
    assertThat(vehicle.getModel()).isEqualTo("Gol");
    assertThat(vehicle.getYear()).isEqualTo(2019);
    assertThat(vehicle.getColor()).isEqualTo("Branco");
    assertThat(vehicle.getPurchasePrice()).isEqualTo(Money.of("25000"));
    assertThat(vehicle.getSalePrice()).isEqualTo(Money.of("32000"));
    assertThat(vehicle.getOrigin()).isEqualTo(VehicleOrigin.CONSIGNED);
    assertThat(vehicle.getStatus()).isEqualTo(status);
    assertThat(vehicle.getStatusChanges()).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(names = {"RESERVED", "SOLD"})
  void blocksUpdateWhenReservedOrSold(VehicleStatus status) {
    var vehicle = restore(status);

    assertThatThrownBy(() -> vehicle.update("Volkswagen", "Gol", 2019, "Branco", Money.of("25000"),
        Money.of("32000"), VehicleOrigin.CONSIGNED))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining(status.name());
    assertThat(vehicle.getBrand()).isEqualTo("Fiat");
  }

  @Test
  void rejectsMissingRequiredFields() {
    var price = Money.of("30000");

    assertThatThrownBy(() -> Vehicle.register(" ", "Uno", 2020, "Prata", price, price, VehicleOrigin.OWNED))
        .hasMessage("A marca é obrigatória");
    assertThatThrownBy(() -> Vehicle.register("Fiat", null, 2020, "Prata", price, price, VehicleOrigin.OWNED))
        .hasMessage("O modelo é obrigatório");
    assertThatThrownBy(() -> Vehicle.register("Fiat", "Uno", 2020, "", price, price, VehicleOrigin.OWNED))
        .hasMessage("A cor é obrigatória");
    assertThatThrownBy(() -> Vehicle.register("Fiat", "Uno", 2020, "Prata", null, price, VehicleOrigin.OWNED))
        .hasMessage("O valor de entrada é obrigatório");
    assertThatThrownBy(() -> Vehicle.register("Fiat", "Uno", 2020, "Prata", price, null, VehicleOrigin.OWNED))
        .hasMessage("O preço de venda é obrigatório");
    assertThatThrownBy(() -> Vehicle.register("Fiat", "Uno", 2020, "Prata", price, price, null))
        .hasMessage("A origem é obrigatória");
  }

  @Test
  void rejectsYearOutOfRange() {
    var price = Money.of("30000");
    var nextYear = Year.now().getValue() + 1;

    assertThat(Vehicle.register("Fiat", "Uno", nextYear, "Prata", price, price, VehicleOrigin.OWNED).getYear())
        .isEqualTo(nextYear);
    assertThatThrownBy(() -> Vehicle.register("Fiat", "Uno", 1899, "Prata", price, price, VehicleOrigin.OWNED))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> Vehicle.register("Fiat", "Uno", nextYear + 1, "Prata", price, price,
        VehicleOrigin.OWNED))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void readmissionRequiresNewPurchasePrice() {
    var vehicle = restore(VehicleStatus.SOLD);

    assertThatThrownBy(() -> vehicle.readmit(null)).hasMessage("O valor de entrada é obrigatório");
    assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.SOLD);
  }
}
