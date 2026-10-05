package br.com.fiap.omnidrive.infrastructure.persistence;

import br.com.fiap.omnidrive.application.gateway.VehicleFilter;
import java.util.ArrayList;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Monta a consulta da busca de veículos a partir do {@link VehicleFilter}. Cada filtro informado vira uma condição,
 * e as condições se combinam com "e". Sem filtros, a consulta aceita todos os veículos.
 *
 * <p>Os trechos de marca e modelo são comparados em minúsculas, e os caracteres curinga do {@code like} ({@code %}
 * e {@code _}) digitados pelo cliente são escapados, para valerem como texto.
 */
final class VehicleSpecifications {

  private static final char ESCAPE = '\\';

  private VehicleSpecifications() {
  }

  static Specification<VehicleEntity> matching(VehicleFilter filter) {
    var specifications = new ArrayList<Specification<VehicleEntity>>();
    if (filter.plate() != null) {
      specifications.add(equalTo("plate", filter.plate().value()));
    }
    if (filter.chassis() != null) {
      specifications.add(equalTo("chassis", filter.chassis().value()));
    }
    if (!filter.statuses().isEmpty()) {
      specifications.add((root, _, _) -> root.get("status").in(filter.statuses()));
    }
    if (filter.brand() != null) {
      specifications.add(containing("brand", filter.brand()));
    }
    if (filter.model() != null) {
      specifications.add(containing("model", filter.model()));
    }
    return Specification.allOf(specifications);
  }

  private static Specification<VehicleEntity> equalTo(
      String attribute,
      String value
  ) {
    return (root, _, builder) -> builder.equal(root.get(attribute), value);
  }

  private static Specification<VehicleEntity> containing(
      String attribute,
      String text
  ) {
    var pattern = "%" + escapeWildcards(text.toLowerCase(Locale.ROOT)) + "%";
    return (root, _, builder) -> builder.like(builder.lower(root.get(attribute)), pattern, ESCAPE);
  }

  private static String escapeWildcards(String text) {
    return text
        .replace(String.valueOf(ESCAPE), String.valueOf(ESCAPE) + ESCAPE)
        .replace("%", ESCAPE + "%")
        .replace("_", ESCAPE + "_");
  }
}
