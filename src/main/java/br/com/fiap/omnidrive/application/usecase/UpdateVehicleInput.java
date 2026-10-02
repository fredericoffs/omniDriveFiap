package br.com.fiap.omnidrive.application.usecase;

import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;

import java.math.BigDecimal;

/**
 * Novos dados de um veículo. Todos os campos são substituídos.
 *
 * @param brand         marca
 * @param model         modelo
 * @param year          ano
 * @param color         cor
 * @param purchasePrice valor de entrada
 * @param salePrice     preço de venda anunciado
 * @param origin        origem do veículo
 * @param version       versão do veículo que o cliente leu antes de editar
 */
public record UpdateVehicleInput(
    String brand,
    String model,
    int year,
    String color,
    BigDecimal purchasePrice,
    BigDecimal salePrice,
    VehicleOrigin origin,
    Long version
) {
}
