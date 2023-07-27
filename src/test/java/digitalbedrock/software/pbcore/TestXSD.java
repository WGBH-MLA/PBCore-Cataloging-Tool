package digitalbedrock.software.pbcore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import digitalbedrock.software.pbcore.core.PBcoreValidator;

class TestXSD {

    private PBcoreValidator val;

    public TestXSD() {

    }

    @BeforeEach
    public void Setup() throws SAXException {

        val = new PBcoreValidator();
    }

    @Test
    void banks_1of5() {

        assertTrue(val
                .isValid(new File(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResource("samples/banks_1of5.xml")
                        .getFile())));
    }

    @Test
    void location_CMS_NUA_umatic00138() {

        assertTrue(val
                .isValid(new File(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResource("samples/location_CMS_NUA_umatic00138.xml")
                        .getFile())));
    }

    @Test
    void location_LTO_NUA_lto60004() {

        assertTrue(val
                .isValid(new File(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResource("samples/location_LTO_NUA_lto60004.xml")
                        .getFile())));
    }

    @Test
    void location_LTO_NUA_reel00445() {

        assertTrue(val
                .isValid(new File(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResource("samples/location_LTO_NUA_reel00445.xml")
                        .getFile())));
    }

    @Test
    void location_simple1_NUA_cass00321_01() {

        assertTrue(val
                .isValid(new File(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResource("samples/location_simple1_NUA_cass00321_01.xml")
                        .getFile())));
    }

    @Test
    void location_simple2_NUA_cass00321() {

        assertTrue(val
                .isValid(new File(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResource("samples/location_simple2_NUA_cass00321.xml")
                        .getFile())));
    }

    @Test
    void pbcore_archival_description() {

        assertTrue(val
                .isValid(new File(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResource("samples/pbcore_archival_description.xml")
                        .getFile())));
    }

    @Test
    void pbcore_asset_management() {

        assertTrue(val
                .isValid(new File(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResource("samples/pbcore_asset_management.xml")
                        .getFile())));
    }

    @Test
    void pbcore_collection() {

        assertTrue(val
                .isValid(new File(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResource("samples/pbcore_collection.xml")
                        .getFile())));
    }

    @Test
    void pbcore_digital_preservation() {

        assertTrue(val
                .isValid(new File(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResource("samples/pbcore_digital_preservation.xml")
                        .getFile())));
    }

    @Test
    void pbcore_digital_preservation_2() {

        assertTrue(val
                .isValid(new File(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResource("samples/pbcore_digital_preservation_2.xml")
                        .getFile())));
    }

    @Test
    void simple_description_document() {

        assertTrue(val
                .isValid(new File(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResource("samples/simple_description_document.xml")
                        .getFile())));
    }

    @Test
    void simple_instantiation_record() {

        assertTrue(val
                .isValid(new File(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResource("samples/simple_instantiation_record.xml")
                        .getFile())));
    }

    @Test
    void _227457_pbcore() {

        assertFalse(val
                .isValid(new File(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResource("samples/227457_pbcore.xml")
                        .getFile())));
    }

    @Test
    void _3087383_pbcore() {

        assertFalse(val
                .isValid(new File(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResource("samples/3087383_pbcore.xml")
                        .getFile())));
    }

    @Test
    void eyesAbernathyRalph() {

        assertFalse(val
                .isValid(new File(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResource("samples/eyesAbernathyRalph.xml")
                        .getFile())));
    }

    @Test
    void eyesAdamsVictoriaGray() {

        assertFalse(val
                .isValid(new File(Thread
                        .currentThread()
                        .getContextClassLoader()
                        .getResource("samples/eyesAdamsVictoriaGray.xml")
                        .getFile())));
    }

}
