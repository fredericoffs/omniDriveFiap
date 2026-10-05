package br.com.fiap.omnidrive.application.gateway;

import br.com.fiap.omnidrive.application.pagination.PageQuery;
import br.com.fiap.omnidrive.application.pagination.PageResult;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistência de veículos, implementada pela camada de infraestrutura.
 */
public interface VehicleGateway {

  /**
   * Grava o veículo e as mudanças de status ainda não persistidas ({@link Vehicle#getStatusChanges()}) na mesma transação.
   *
   * @return o veículo como ficou gravado, com a versão nova e sem mudanças de status pendentes. Novas alterações devem partir dele, e não do objeto
   * recebido.
   * @throws br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException se o veículo foi alterado por outra operação depois de lido
   * @throws br.com.fiap.omnidrive.domain.exception.AlreadyExistsException    se outro veículo já tiver o mesmo chassi ou a mesma placa
   */
  Vehicle save(Vehicle vehicle);

  /**
   * Indica se há um veículo com o chassi informado.
   */
  boolean existsByChassis(Chassis chassis);

  /**
   * Indica se há um veículo com a placa informada.
   */
  boolean existsByPlate(Plate plate);

  /**
   * Busca um veículo pelo id.
   */
  Optional<Vehicle> findById(UUID id);

  /**
   * Lista uma página dos veículos com o status informado, ordenados pelo preço de venda, do menor para o maior. Preços
   * iguais são desempatados pelo id, para que a ordem não mude entre uma página e outra.
   */
  PageResult<Vehicle> findByStatusOrderBySalePrice(
      VehicleStatus status,
      PageQuery pageQuery
  );

  /**
   * Lista uma página dos veículos Vendidos, ordenados pelo valor da venda atual, do menor para o maior. Valores iguais
   * são desempatados pelo id do veículo, para que a ordem não mude entre uma página e outra.
   */
  PageResult<Vehicle> findSoldOrderBySaleAmount(PageQuery pageQuery);

  /**
   * Busca uma página dos veículos que atendem a todos os filtros informados, em qualquer status. O resultado vem do
   * cadastro mais recente para o mais antigo, com desempate pelo id.
   */
  PageResult<Vehicle> search(
      VehicleFilter filter,
      PageQuery pageQuery
  );
}
