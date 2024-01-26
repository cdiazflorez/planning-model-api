package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.step;

import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.step.Options.TAG_BY;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlannedUnitsService.Input;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlannedUnitsService.TaggedUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * GroupByBuilder builds a reduction function over the stream of {@link TaggedUnit}.
 * The reduction operation groups the units by the tags specified in the input options, discarding those tags
 * not specified in the tag_by option.
 * - If the tag_by option is not present, then no reduction operation is applied.
 * - If more than unit have the same tag values, the units are summed.
 * - If some unit does not have a tag that is specified in the options, then an empty string is used as the tag value.
 */
public class GroupByBuilder {

  private static Map<String, String> extractTags(final String[] tagBy, Map<String, String> tags) {
    return Arrays.stream(tagBy)
        .collect(toMap(tag -> tag, tag -> tags.getOrDefault(tag, "")));
  }

  public UnaryOperator<Stream<TaggedUnit>> build(final Input input) {
    final var tags = Optional.of(input)
        .map(Input::options)
        .map(options -> options.get(TAG_BY))
        .map(tagBy -> tagBy.split(","))
        .orElse(new String[0]);

    return taggedUnits -> {
      if (tags.length == 0) {
        return taggedUnits;
      }

      return taggedUnits.collect(
              toMap(
                  unit -> extractTags(tags, unit.tags()),
                  TaggedUnit::quantity,
                  Double::sum
              )
          )
          .entrySet()
          .stream()
          .map(entry -> new TaggedUnit(entry.getValue(), entry.getKey()));
    };
  }
}
