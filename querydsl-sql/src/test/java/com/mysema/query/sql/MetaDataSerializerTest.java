/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.query.sql;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.tools.JavaCompiler;

import junit.framework.Assert;

import org.junit.Test;

import com.mysema.codegen.CodeWriter;
import com.mysema.codegen.SimpleCompiler;
import com.mysema.query.AbstractJDBCTest;
import com.mysema.query.codegen.EntityType;
import com.mysema.query.codegen.Property;
import com.mysema.query.codegen.SerializerConfig;

public class MetaDataSerializerTest extends AbstractJDBCTest{

    @Test
    public void testGeneration() throws Exception {
        // normal settings

        statement.execute("drop table employee if exists");
        statement.execute("drop table survey if exists");
        statement.execute("drop table date_test if exists");
        statement.execute("drop table date_time_test if exists");

        statement.execute("create table survey (id int, name varchar(30), "
                + "CONSTRAINT PK_survey PRIMARY KEY (id, name))");
        statement.execute("create table date_test (d date)");
        statement.execute("create table date_time_test (dt datetime)");
        statement.execute("create table employee("
                + "id INT, "
                + "firstname VARCHAR(50), "
                + "lastname VARCHAR(50), "
                + "salary DECIMAL(10, 2), "
                + "datefield DATE, "
                + "timefield TIME, "
                + "superior_id int, "
                + "survey_id int, "
                + "survey_name varchar(30), "
                + "CONSTRAINT PK_employee PRIMARY KEY (id), "
                + "CONSTRAINT FK_survey FOREIGN KEY (survey_id, survey_name) REFERENCES survey(id,name), "
                + "CONSTRAINT FK_superior FOREIGN KEY (superior_id) REFERENCES employee(id))");

        String namePrefix = "Q";
        NamingStrategy namingStrategy = new DefaultNamingStrategy();
        // customization of serialization
        MetaDataSerializer serializer = new MetaDataSerializer(namePrefix, namingStrategy){

            @Override
            protected void introImports(CodeWriter writer, SerializerConfig config, EntityType model) throws IOException {
            super.introImports(writer, config, model);
            // adds additional imports
            writer.imports(List.class, Arrays.class);
            }

            @Override
            protected void serializeProperties(EntityType model,  SerializerConfig config, CodeWriter writer) throws IOException {
            super.serializeProperties(model, config, writer);
            StringBuilder paths = new StringBuilder();
            for (Property property : model.getProperties()){
                if (paths.length() > 0){
                paths.append(", ");
                }
                paths.append(property.getEscapedName());
            }
            // adds accessors for all fields
            writer.publicFinal("List<Expr<?>>", "exprs", "Arrays.<Expr<?>>asList(" + paths.toString() + ")");
            writer.publicFinal("List<Path<?>>", "paths", "Arrays.<Path<?>>asList(" + paths.toString() + ")");
            }

        };
        MetaDataExporter exporter = new MetaDataExporter(
            namePrefix,
            "test",
            null,
            null,
            new File("target/cust"),
            namingStrategy,
            serializer);

        exporter.export(connection.getMetaData());

        JavaCompiler compiler = new SimpleCompiler();
        Set<String> classes = exporter.getClasses();
        int compilationResult = compiler.run(null, null, null, classes.toArray(new String[classes.size()]));
        if(compilationResult == 0){
            System.out.println("Compilation is successful");
        }else{
            Assert.fail("Compilation Failed");
        }
    }

}