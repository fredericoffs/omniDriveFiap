package br.com.fiap.omnidrive.application.usecase;

import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;

import java.util.Set;

/**
 * Filtros informados na busca de veículos, todos opcionais. Texto nulo ou em branco não filtra.
 *
 * @param plate    placa, no padrão antigo ou no Mercosul, com ou sem hífen
 * @param chassis  chassi
 * @param statuses status aceitos; nulo ou vazio aceita todos
 * @param brand    trecho da marca
 * @param model    trecho do modelo
 */
public record SearchVehiclesInput(
    String plate,
    String chassis,
    Set<VehicleStatus> statuses,
    String brand,
    String model
) {
}
