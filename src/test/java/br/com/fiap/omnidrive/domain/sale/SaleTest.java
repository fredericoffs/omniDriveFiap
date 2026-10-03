package br.com.fiap.omnidrive.domain.sale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.omnidrive.domain.common.Cpf;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Testes da criação de uma {@link Sale}.
 */
class SaleTest {

  private static final Cpf BUYER_CPF = new Cpf("52998224725");

  private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

  private static final Vehicle VEHICLE = Vehicle.restore(
      UUID.randomUUID(),
      new Chassis("9BWZZZ377LT004251"),
      new VehicleDetails(
          "Fiat",
          "Uno",
          2020,
          "Prata",
          new Plate("ABC1D23"),
          45000,
          Money.of("30000"),
          Money.of("40000"),
          VehicleOrigin.OWNED
      ),
      VehicleStatus.AVAILABLE,
      null,
      0
  );

  @Test
  void copiesVehicleSalePrice() {
    var sale = Sale.create(VEHICLE, BUYER_CPF, TODAY);

    assertThat(sale.getId()).isNotNull();
    assertThat(sale.getVehicleId()).isEqualTo(VEHICLE.getId());
    assertThat(sale.getBuyerCpf()).isEqualTo(BUYER_CPF);
    assertThat(sale.getSaleDate()).isEqualTo(TODAY);
    assertThat(sale.getAmount()).isEqualTo(Money.of("40000"));
  }

  @Test
  void acceptsPastDate() {
    var yesterday = TODAY.minusDays(1);

    assertThat(Sale.create(VEHICLE, BUYER_CPF, yesterday).getSaleDate()).isEqualTo(yesterday);
  }

  @Test
  void rejectsMissingBuyerCpf() {
    assertThatThrownBy(() -> Sale.create(VEHICLE, null, TODAY))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("O CPF do comprador é obrigatório");
  }

  @Test
  void rejectsMissingSaleDate() {
    assertThatThrownBy(() -> Sale.create(VEHICLE, BUYER_CPF, null))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("A data da venda é obrigatória");
  }

  @Test
  void rejectsFutureSaleDate() {
    var tomorrow = TODAY.plusDays(1);

    assertThatThrownBy(() -> Sale.create(VEHICLE, BUYER_CPF, tomorrow))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("A data da venda não pode estar no futuro");
  }
}
