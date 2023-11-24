package io.github.booster.web.handler;

import arrow.core.Option;
import io.github.booster.commons.compression.CompressionAlgorithm;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;

public interface AcceptEncodingParser {

    @AllArgsConstructor
    @Getter
    class Encoding {

        private CompressionAlgorithm algorithm;

        private double weight;

        public static Option<Encoding> build(
                String encoding,
                Double weight
        ) {
            if (StringUtils.isBlank(encoding)) {
                return Option.fromNullable(null);
            }

            CompressionAlgorithm algorithm = CompressionAlgorithm.Companion.findAlgorithm(encoding);

            if (weight == null) {
                weight = 1.0;
            }
            return Option.fromNullable(
                    new Encoding(
                            algorithm,
                            weight
                    )
            );
        }
    }

    Logger log = LoggerFactory.getLogger(AcceptEncodingParser.class);

    static CompressionAlgorithm findCompressionAlgorithm(String acceptEncoding, Set<String> allowedAlgorithms) {
        if (StringUtils.isBlank(acceptEncoding)) {
            return CompressionAlgorithm.NONE;
        }

        String[] encodings = acceptEncoding.split(",");

        return Stream.of(encodings)
                .map(AcceptEncodingParser::parseEncoding)
                .filter(Option::isDefined)
                .map(Option::orNull)
                .collect(groupingBy(Encoding::getAlgorithm))
                .entrySet()
                .stream()
                .filter(entry -> allowedAlgorithms.contains(entry.getKey().getAlgorithm()))
                .map(Map.Entry::getValue)
                .map(AcceptEncodingParser::findMaxWeight)
                .filter(Option::isDefined)
                .map(Option::orNull)
                .max(Comparator.comparingDouble(Encoding::getWeight))
                .orElse(new Encoding(CompressionAlgorithm.NONE, 1.0))
                .getAlgorithm();
    }

    static Option<Encoding> findMaxWeight(List<Encoding> encodings) {
        return Option.fromNullable(
                encodings.stream()
                        .max(Comparator.comparingDouble(Encoding::getWeight))
                        .orElse(null)
        );
    }

    static Option<Encoding> parseEncoding(String encoding) {
        if (encoding == null) {
            return Option.fromNullable(null);
        }
        String[] segments = encoding.trim().split(";");
        if (segments.length < 1 || segments.length > 2) {
            return Option.fromNullable(null);
        }

        if (segments.length == 1) {
            return Encoding.build(segments[0].trim(), 1.0);
        } else {
            String[] weightSegments = segments[1].trim().split("=");
            if (weightSegments.length != 2) {
                return Option.fromNullable(null);
            }
            try {
                double weight = Double.parseDouble(weightSegments[1].trim());
                return Encoding.build(segments[0].trim(), weight);
            } catch (NumberFormatException e) {
                log.error("cannot parse weight", e);
                return Option.fromNullable(null);
            }
        }
    }
}
