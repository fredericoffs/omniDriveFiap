package br.com.fiap.omnidrive.infrastructure.web.vehicle;

import br.com.fiap.omnidrive.application.usecase.vehicle.SearchVehiclesInput;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.Set;

/**
 * Filtros da busca de veículos, todos opcionais e combinados entre si. Texto em branco não filtra.
 *
 * @param plate   placa exata, com ou sem hífen
 * @param chassis chassi exato
 * @param status  status aceitos, com o parâmetro repetido para mais de um
 * @param brand   trecho da marca
 * @param model   trecho do modelo
 */
public record SearchVehiclesParams(

    @Parameter(
        description = "Placa exata, no padrão antigo (ABC-1234) ou no Mercosul (ABC1D23), com ou sem hífen e sem "
            + "diferenciar maiúsculas de minúsculas",
        example = "ABC1D23"
    )
    String plate,

    @Parameter(description = "Chassi exato, sem diferenciar maiúsculas de minúsculas", example = "9BWZZZ377LT004251")
    String chassis,

    @Parameter(
        description = "Status aceitos. Repita o parâmetro para mais de um (status=RECEIVED&status=IN_PREPARATION). "
            + "Sem ele, a busca aceita todos os status"
    )
    Set<VehicleStatus> status,

    @Parameter(description = "Trecho da marca, sem diferenciar maiúsculas de minúsculas", example = "hon")
    String brand,

    @Parameter(description = "Trecho do modelo, sem diferenciar maiúsculas de minúsculas", example = "civ")
    String model
) {

  SearchVehiclesInput toInput() {
    return new SearchVehiclesInput(
        plate,
        chassis,
        status,
        brand,
        model
    );
  }
}
