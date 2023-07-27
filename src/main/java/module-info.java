module digitalbedrock.software.pbcore {
    requires javafx.controls;
    requires org.controlsfx.controls;
    requires javafx.fxml;
    requires com.opencsv;
    requires java.logging;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign;
    requires org.apache.commons.text;
    requires lucene.core;
    requires java.xml;
    requires javafx.autocomplete.field;
    requires javafx.web;
    requires java.desktop;
    requires org.apache.commons.io;
    requires lombok;

    requires java.xml.bind;
    requires com.sun.xml.bind;

    opens digitalbedrock.software.pbcore to javafx.fxml;
    opens digitalbedrock.software.pbcore.controllers to javafx.fxml;
    opens digitalbedrock.software.pbcore.components to javafx.fxml;
    opens digitalbedrock.software.pbcore.components.editor to javafx.fxml;
    opens digitalbedrock.software.pbcore.core to javafx.fxml, com.fasterxml.jackson.databind;
    opens digitalbedrock.software.pbcore.core.models to com.fasterxml.jackson.databind;
    opens digitalbedrock.software.pbcore.core.models.entity to com.fasterxml.jackson.databind;
    opens digitalbedrock.software.pbcore.core.models.document to java.xml.bind;
    opens digitalbedrock.software.pbcore.utils to com.fasterxml.jackson.databind;

    exports digitalbedrock.software.pbcore;
    exports digitalbedrock.software.pbcore.controllers;
    exports digitalbedrock.software.pbcore.core;
    exports digitalbedrock.software.pbcore.core.models;
    exports digitalbedrock.software.pbcore.core.models.entity;
    exports digitalbedrock.software.pbcore.components;
    exports digitalbedrock.software.pbcore.lucene;
    exports digitalbedrock.software.pbcore.listeners;
    exports digitalbedrock.software.pbcore.parsers;
    exports digitalbedrock.software.pbcore.utils;

}
