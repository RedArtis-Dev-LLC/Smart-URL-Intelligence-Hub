package com.smarturl.hub.link.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarturl.hub.link.config.LinkProperties;
import com.smarturl.hub.link.config.LinkProperties.Amqp;
import com.smarturl.hub.link.config.LinkProperties.Outbox;
import com.smarturl.hub.link.config.LinkProperties.Redirect;
import com.smarturl.hub.link.config.LinkProperties.ShortCode;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShortCodeGeneratorTest {

    ShortCodeGenerator generator;

    @BeforeEach
    void setUp() {
        var properties = new LinkProperties(
                new ShortCode(7, 5),
                new Redirect(3),
                new Outbox(2000, 50, "0 0 3 * * *", Duration.ofDays(7)),
                new Amqp("click.events", "click.event"));
        generator = new ShortCodeGenerator(properties);
    }

    @Test
    void generate_returnsBase62CodeOfConfiguredLength() {
        String code = generator.generate();

        assertThat(code).hasSize(7).matches("^[A-Za-z0-9]{7}$");
    }

    @Test
    void generate_producesDistinctCodesAcrossManyCalls() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(generator.generate());
        }
        assertThat(codes).hasSizeGreaterThan(995);
    }
}
