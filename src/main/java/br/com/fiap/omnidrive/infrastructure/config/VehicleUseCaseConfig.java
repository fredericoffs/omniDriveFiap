package br.com.fiap.omnidrive.infrastructure.config;

import br.com.fiap.omnidrive.application.gateway.SaleGateway;
import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.application.usecase.DeleteVehicle;
import br.com.fiap.omnidrive.application.usecase.GetVehicle;
import br.com.fiap.omnidrive.application.usecase.ListSoldVehicles;
import br.com.fiap.omnidrive.application.usecase.ListVehiclesForSale;
import br.com.fiap.omnidrive.application.usecase.ReadmitVehicle;
import br.com.fiap.omnidrive.application.usecase.ReleaseVehicleForSale;
import br.com.fiap.omnidrive.application.usecase.SearchVehicles;
import br.com.fiap.omnidrive.application.usecase.StartVehiclePreparation;
import br.com.fiap.omnidrive.application.usecase.vehicle.RegisterVehicle;
import br.com.fiap.omnidrive.application.usecase.vehicle.UpdateVehicle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra os casos de uso de veículo como beans. Os casos de uso são Java puro e não conhecem o Spring; a ligação com a infraestrutura acontece só
 * aqui.
 */
@Configuration(proxyBeanMethods = false)
public class VehicleUseCaseConfig {

  @Bean
  RegisterVehicle registerVehicle(VehicleGateway vehicleGateway) {
    return new RegisterVehicle(vehicleGateway);
  }

  @Bean
  UpdateVehicle updateVehicle(VehicleGateway vehicleGateway) {
    return new UpdateVehicle(vehicleGateway);
  }

  @Bean
  GetVehicle getVehicle(VehicleGateway vehicleGateway) {
    return new GetVehicle(vehicleGateway);
  }

  @Bean
  DeleteVehicle deleteVehicle(
      VehicleGateway vehicleGateway,
      SaleGateway saleGateway
  ) {
    return new DeleteVehicle(
        vehicleGateway,
        saleGateway
    );
  }

  @Bean
  StartVehiclePreparation startVehiclePreparation(VehicleGateway vehicleGateway) {
    return new StartVehiclePreparation(vehicleGateway);
  }

  @Bean
  ReleaseVehicleForSale releaseVehicleForSale(VehicleGateway vehicleGateway) {
    return new ReleaseVehicleForSale(vehicleGateway);
  }

  @Bean
  ReadmitVehicle readmitVehicle(VehicleGateway vehicleGateway) {
    return new ReadmitVehicle(vehicleGateway);
  }

  @Bean
  SearchVehicles searchVehicles(VehicleGateway vehicleGateway) {
    return new SearchVehicles(vehicleGateway);
  }

  @Bean
  ListVehiclesForSale listVehiclesForSale(VehicleGateway vehicleGateway) {
    return new ListVehiclesForSale(vehicleGateway);
  }

  @Bean
  ListSoldVehicles listSoldVehicles(
      VehicleGateway vehicleGateway,
      SaleGateway saleGateway
  ) {
    return new ListSoldVehicles(
        vehicleGateway,
        saleGateway
    );
  }
}
