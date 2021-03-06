/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.sql.expression.function.scalar.datetime;

import org.elasticsearch.xpack.sql.expression.Expression;
import org.elasticsearch.xpack.sql.expression.Expressions;
import org.elasticsearch.xpack.sql.expression.FieldAttribute;
import org.elasticsearch.xpack.sql.expression.function.scalar.datetime.NamedDateTimeProcessor.NameExtractor;
import org.elasticsearch.xpack.sql.expression.gen.pipeline.Pipe;
import org.elasticsearch.xpack.sql.expression.gen.pipeline.UnaryPipe;
import org.elasticsearch.xpack.sql.expression.gen.script.ScriptTemplate;
import org.elasticsearch.xpack.sql.tree.Location;
import org.elasticsearch.xpack.sql.type.DataType;
import org.elasticsearch.xpack.sql.util.StringUtils;

import java.util.Locale;
import java.util.TimeZone;

import static java.lang.String.format;
import static org.elasticsearch.xpack.sql.expression.gen.script.ParamsBuilder.paramsBuilder;

/*
 * Base class for "named" date/time functions like month_name and day_name
 */
abstract class NamedDateTimeFunction extends BaseDateTimeFunction {

    private final NameExtractor nameExtractor;

    NamedDateTimeFunction(Location location, Expression field, TimeZone timeZone, NameExtractor nameExtractor) {
        super(location, field, timeZone);
        this.nameExtractor = nameExtractor;
    }

    @Override
    protected Object doFold(long millis, String tzId) {
        return nameExtractor.extract(millis, tzId);
    }

    @Override
    public ScriptTemplate scriptWithField(FieldAttribute field) {
        return new ScriptTemplate(
                formatTemplate(format(Locale.ROOT, "{sql}.%s(doc[{}].value, {})",
                        StringUtils.underscoreToLowerCamelCase(nameExtractor.name()))),
                paramsBuilder()
                  .variable(field.name())
                  .variable(timeZone().getID()).build(),
                dataType());
    }

    @Override
    protected final Pipe makePipe() {
        return new UnaryPipe(location(), this, Expressions.pipe(field()), new NamedDateTimeProcessor(nameExtractor, timeZone()));
    }

    @Override
    public DataType dataType() {
        return DataType.KEYWORD;
    }
}