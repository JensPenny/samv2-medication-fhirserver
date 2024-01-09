package be.fhir.penny.terminology;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class FileBackedFileDescriptor implements FileDescriptor {
    private final File myNextFile;

    public FileBackedFileDescriptor(File theNextFile) {
        myNextFile = theNextFile;
    }

    @Override
    public String getFilename() {
        return myNextFile.getAbsolutePath();
    }

    @Override
    public InputStream getInputStream() {
        try {
            return new FileInputStream(myNextFile);
        } catch (FileNotFoundException theE) {
            throw new InternalErrorException(Msg.code(1142) + theE);
        }
    }

    @Override
    public String toString() {
        return "FileBackedFileDescriptor{" +
                "myNextFile=" + myNextFile +
                '}';
    }
}
