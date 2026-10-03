package br.com.fiap.omnidrive.application.usecase;

import java.math.BigDecimal;

/**
 * Dados para a reentrada de um veículo vendido na loja.
 *
 * @param purchasePrice novo valor de entrada: quanto a loja pagou para recomprar o veículo, ou o valor acordado com
 *                      o dono se consignado
 * @param mileage       quilometragem na volta, em km; deve ser maior que a atual
 */
public record ReadmitVehicleInput(
    BigDecimal purchasePrice,
    Integer mileage
) {
}
