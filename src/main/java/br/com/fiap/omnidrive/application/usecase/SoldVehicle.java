package br.com.fiap.omnidrive.application.usecase;

import br.com.fiap.omnidrive.domain.sale.Sale;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;

import java.util.Objects;

/**
 * Veículo vendido, com a venda atual dele.
 *
 * @param vehicle o veículo, com status Vendido
 * @param sale    a venda atual do veículo
 */
public record SoldVehicle(
    Vehicle vehicle,
    Sale sale
) {

  public SoldVehicle {
    Objects.requireNonNull(vehicle);
    Objects.requireNonNull(sale);
  }
}
