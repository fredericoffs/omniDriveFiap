package br.com.fiap.omnidrive.application.usecase;

import java.math.BigDecimal;

/**
 * Dados para a reentrada de um veículo vendido na loja.
 *
 * @param purchasePrice novo valor de entrada: quanto a loja pagou para recomprar o veículo, ou o valor acordado com
 *                      o dono se consignado
 */
public record ReadmitVehicleInput(BigDecimal purchasePrice) {
}
