package br.com.fiap.omnidrive.application.usecase;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Dados para efetuar a venda de um veículo.
 *
 * @param vehicleId veículo vendido, que precisa estar Disponível
 * @param buyerCpf  CPF do comprador, com ou sem pontuação
 * @param saleDate  data da venda, que não pode estar no futuro
 */
public record CreateSaleInput(
    UUID vehicleId,
    String buyerCpf,
    LocalDate saleDate
) {
}
