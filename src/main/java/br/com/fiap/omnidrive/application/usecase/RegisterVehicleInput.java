package br.com.fiap.omnidrive.application.usecase;

import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;

import java.math.BigDecimal;

/**
 * Dados para cadastrar um veículo.
 *
 * @param chassis       chassi: VIN de 17 caracteres, ou outro formato para veículos até 1986
 * @param plate         placa, no padrão antigo ou no Mercosul
 * @param mileage       quilometragem, em km
 * @param brand         marca
 * @param model         modelo
 * @param year          ano
 * @param color         cor
 * @param purchasePrice valor de entrada: quanto a loja pagou ou acordou com o dono, se consignado
 * @param salePrice     preço de venda anunciado
 * @param origin        origem do veículo
 */
public record RegisterVehicleInput(
    String chassis,
    String plate,
    int mileage,
    String brand,
    String model,
    int year,
    String color,
    BigDecimal purchasePrice,
    BigDecimal salePrice,
    VehicleOrigin origin
) {
}
