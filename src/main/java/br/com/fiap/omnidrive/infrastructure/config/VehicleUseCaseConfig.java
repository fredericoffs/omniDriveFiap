package br.com.fiap.omnidrive.infrastructure.config;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.application.usecase.GetVehicle;
import br.com.fiap.omnidrive.application.usecase.ListVehiclesForSale;
import br.com.fiap.omnidrive.application.usecase.RegisterVehicle;
import br.com.fiap.omnidrive.application.usecase.ReleaseVehicleForSale;
import br.com.fiap.omnidrive.application.usecase.StartVehiclePreparation;
import br.com.fiap.omnidrive.application.usecase.UpdateVehicle;
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
  StartVehiclePreparation startVehiclePreparation(VehicleGateway vehicleGateway) {
    return new StartVehiclePreparation(vehicleGateway);
  }

  @Bean
  ReleaseVehicleForSale releaseVehicleForSale(VehicleGateway vehicleGateway) {
    return new ReleaseVehicleForSale(vehicleGateway);
  }

  @Bean
  ListVehiclesForSale listVehiclesForSale(VehicleGateway vehicleGateway) {
    return new ListVehiclesForSale(vehicleGateway);
  }
}
