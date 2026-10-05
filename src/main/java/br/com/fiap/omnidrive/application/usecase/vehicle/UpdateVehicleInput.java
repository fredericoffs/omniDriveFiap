package br.com.fiap.omnidrive.application.usecase.vehicle;

import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;

import java.math.BigDecimal;

/**
 * Novos dados de um veículo. Todos os campos são substituídos.
 *
 * @param plate         placa, no padrão antigo ou no Mercosul
 * @param mileage       quilometragem, em km
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
    String plate,
    int mileage,
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
