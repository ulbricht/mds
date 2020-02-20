package org.datacite.mds.web;

import org.datacite.mds.domain.Allocator;
import org.datacite.mds.domain.Datacentre;
import org.datacite.mds.domain.Dataset;
import org.datacite.mds.domain.Metadata;
import org.datacite.mds.domain.Prefix;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.FormattingConversionServiceFactoryBean;
import org.apache.log4j.Logger;

import java.util.HashSet;

//@RooConversionService
//(disabled due to https://jira.springsource.org/browse/ROO-2593
public class ApplicationConversionServiceFactoryBean extends FormattingConversionServiceFactoryBean {

    private static Logger log = Logger.getLogger(Allocator.class);

    public ApplicationConversionServiceFactoryBean() {
        super();

        HashSet<Converter> converterset = new HashSet<Converter>();

        converterset.add(getByteArrayConverter());
        converterset.add(getSimpleAllocatorConverter());
        converterset.add(getSimpleDatacentreConverter());
        converterset.add(getSimpleDatasetConverter());
        converterset.add(getSimpleMetadataConverter());
        converterset.add(getSimplePrefixConverter());
        setConverters(converterset);
    }

    public static Converter<byte[], String> getByteArrayConverter() {
        return new Converter<byte[], String>() {
            public String convert(byte[] bytes) {
                log.debug("Byteconverter");
                return new String(bytes);
            }
        };
    }

    public static Converter<Dataset, String> getSimpleDatasetConverter() {
        return new Converter<Dataset, String>() {
            public String convert(Dataset dataset) {
                log.debug("Datasetconverter");
                return dataset.getDoi();
            }
        };
    }

    public static Converter<Datacentre, String> getSimpleDatacentreConverter() {
        return new Converter<Datacentre, String>() {
            public String convert(Datacentre datacentre) {
                log.debug("Datacentreconverter");
                return datacentre.getSymbol();
            }
        };
    }

    public static Converter<Allocator, String> getSimpleAllocatorConverter() {
        return new Converter<Allocator, String>() {
            public String convert(Allocator allocator) {
                log.debug("Allocatorconverter");
                return allocator.getSymbol();
            }
        };
    }

    public static Converter<Prefix, String> getSimplePrefixConverter() {
        return new Converter<Prefix, String>() {
            public String convert(Prefix prefix) {
                log.debug("Prefixconverter");
                return prefix.getPrefix();
            }
        };
    }

    public static Converter<Metadata, String> getSimpleMetadataConverter() {
        return new Converter<Metadata, String>() {
            public String convert(Metadata metadata) {
                log.debug("Metadataconverter");
                return metadata.getMetadataVersion() + " (" + metadata.getCreated() + ")";
            }
        };
    }
}
