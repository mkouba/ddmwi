package com.github.mkouba.ddmwi.ctrl;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class FileImportForm {

    @RestForm
    public FileUpload importFile;

}
