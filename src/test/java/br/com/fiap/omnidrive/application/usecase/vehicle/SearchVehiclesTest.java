package br.com.fiap.omnidrive.application.usecase.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.application.gateway.VehicleFilter;
import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.application.pagination.PageQuery;
import br.com.fiap.omnidrive.application.pagination.PageResult;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes do caso de uso {@link SearchVehicles}.
 */
@ExtendWith(MockitoExtension.class)
class SearchVehiclesTest {

  private static final PageQuery PAGE_QUERY = new PageQuery(1, 10);

  @Mock
  private VehicleGateway vehicleGateway;

  private SearchVehicles searchVehicles;

  @BeforeEach
  void setUp() {
    searchVehicles = new SearchVehicles(vehicleGateway);
  }

  @Test
  void searchesWithNormalizedPlateAndChassisAndStrippedText() {
    var input = new SearchVehiclesInput(
        " abc-1234 ",
        " 9bwzzz377lt004251 ",
        Set.of(VehicleStatus.AVAILABLE, VehicleStatus.RESERVED),
        " Fia ",
        " Un "
    );
    var filter = new VehicleFilter(
        new Plate("ABC1234"),
        new Chassis("9BWZZZ377LT004251"),
        Set.of(VehicleStatus.AVAILABLE, VehicleStatus.RESERVED),
        "Fia",
        "Un"
    );
    var page = new PageResult<Vehicle>(List.of(), 1, 10, 10);
    when(vehicleGateway.search(filter, PAGE_QUERY)).thenReturn(page);

    assertThat(searchVehicles.execute(input, PAGE_QUERY)).isEqualTo(page);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   "})
  void ignoresMissingFilters(String text) {
    var input = new SearchVehiclesInput(text, text, null, text, text);
    var page = new PageResult<Vehicle>(List.of(), 1, 10, 0);
    when(vehicleGateway.search(new VehicleFilter(null, null, Set.of(), null, null), PAGE_QUERY)).thenReturn(page);

    assertThat(searchVehicles.execute(input, PAGE_QUERY)).isEqualTo(page);
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
      "AB-12 | | A placa deve seguir o padrão antigo (ABC-1234) ou o Mercosul (ABC1D23)",
      " | 9BWZZZ377LT00425I | O VIN deve conter só letras e números, sem I, O e Q"
  })
  void rejectsInvalidPlateOrChassisWithoutSearching(
      String plate,
      String chassis,
      String message
  ) {
    var input = new SearchVehiclesInput(plate, chassis, Set.of(), null, null);

    assertThatThrownBy(() -> searchVehicles.execute(input, PAGE_QUERY))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(message);
    verifyNoInteractions(vehicleGateway);
  }
}
