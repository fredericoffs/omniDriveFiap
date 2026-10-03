package br.com.fiap.omnidrive.domain.vehicle;

import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;

import java.time.Year;
import java.time.ZoneId;

/**
 * Dados cadastrais de um veículo, informados no cadastro e substituídos por inteiro na edição. Os textos são
 * gravados sem espaços nas pontas, o ano fica entre 1900 e o ano seguinte ao atual e a quilometragem não é negativa.
 * O chassi não faz parte destes dados porque identifica o veículo e não muda na edição.
 *
 * @param mileage       quilometragem, em km
 * @param purchasePrice valor de entrada: quanto a loja pagou ou acordou com o dono, se consignado
 * @param salePrice     preço de venda anunciado
 * @throws BusinessRuleException se algum dado obrigatório faltar ou for inválido
 */
public record VehicleDetails(
    String brand,
    String model,
    int year,
    String color,
    Plate plate,
    int mileage,
    Money purchasePrice,
    Money salePrice,
    VehicleOrigin origin
) {

  private static final int MIN_YEAR = 1900;

  public VehicleDetails {
    brand = requireText(brand, "A marca é obrigatória");
    model = requireText(model, "O modelo é obrigatório");
    year = validateYear(year);
    color = requireText(color, "A cor é obrigatória");
    plate = require(plate, "A placa é obrigatória");
    if (mileage < 0) {
      throw new BusinessRuleException("A quilometragem não pode ser negativa");
    }
    purchasePrice = require(purchasePrice, "O valor de entrada é obrigatório");
    salePrice = require(salePrice, "O preço de venda é obrigatório");
    origin = require(origin, "A origem é obrigatória");
  }

  private static int validateYear(int year) {
    var maxYear = Year.now(ZoneId.of("America/Sao_Paulo")).getValue() + 1;
    if (year < MIN_YEAR || year > maxYear) {
      throw new BusinessRuleException("O ano deve estar entre " + MIN_YEAR + " e " + maxYear);
    }
    return year;
  }

  private static String requireText(
      String value,
      String message
  ) {
    if (value == null || value.isBlank()) {
      throw new BusinessRuleException(message);
    }
    return value.strip();
  }

  private static <T> T require(
      T value,
      String message
  ) {
    if (value == null) {
      throw new BusinessRuleException(message);
    }
    return value;
  }

  /**
   * @return uma cópia com o novo valor de entrada e a nova quilometragem, usada na reentrada do veículo
   */
  public VehicleDetails withReadmission(
      Money newPurchasePrice,
      int newMileage
  ) {
    return new VehicleDetails(
        brand,
        model,
        year,
        color,
        plate,
        newMileage,
        newPurchasePrice,
        salePrice,
        origin
    );
  }
}
