package br.com.fiap.omnidrive.application.usecase;

import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;

import java.math.BigDecimal;

/**
 * Dados para cadastrar um veículo.
 *
 * @param brand         marca
 * @param model         modelo
 * @param year          ano
 * @param color         cor
 * @param purchasePrice valor de entrada: quanto a loja pagou ou acordou com o dono, se consignado
 * @param salePrice     preço de venda anunciado
 * @param origin        origem do veículo
 */
public record RegisterVehicleInput(
    String brand,
    String model,
    int year,
    String color,
    BigDecimal purchasePrice,
    BigDecimal salePrice,
    VehicleOrigin origin
) {
}
