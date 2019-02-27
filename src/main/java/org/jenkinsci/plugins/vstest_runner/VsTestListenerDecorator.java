package org.jenkinsci.plugins.vstest_runner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hudson.console.LineTransformationOutputStream;
import hudson.model.TaskListener;
import hudson.console.ConsoleNote;
import java.nio.ByteBuffer;

/**
 * @author a.filatov
 * 31.03.2014.
 */
public class VsTestListenerDecorator extends LineTransformationOutputStream {

    private final static String TRX_PATTERN = "^Results File: (.*\\.trx)$";
    private final static int TRX_GROUP = 1;

    private final static String ATTACHMENTS_PATTERN = "^Attachments:\\s*$";

    private final static String COVERAGE_PATTERN = "^\\s*(.*\\.coverage)$";
    private final static int COVERAGE_GROUP = 1;

    private final OutputStream out;
    private final Charset charset;

    private final Pattern trxPattern;
    private final Pattern attachmentsPattern;
    private final Pattern coveragePattern;

    private boolean attachmentsSection;

    private String trxFile;
    private String coverageFile;

    public VsTestListenerDecorator(OutputStream out, Charset charset) throws FileNotFoundException {
        this.out = out;
        this.charset = charset;

        trxFile = null;
        coverageFile = null;

        this.attachmentsSection = false;
        this.trxPattern = Pattern.compile(TRX_PATTERN);
        this.attachmentsPattern = Pattern.compile(ATTACHMENTS_PATTERN);
        this.coveragePattern = Pattern.compile(COVERAGE_PATTERN);
    }

    public String getTrxFile() {
        return this.trxFile;
    }

    public String getCoverageFile() {
        return this.coverageFile;
    }

    @Override
    protected void eol(byte[] bytes, int len) throws IOException {

        if (this.out == null) {
            return;
        }

        String line = ConsoleNote.removeNotes(charset.decode(ByteBuffer.wrap(bytes, 0, len)).toString());

        Matcher trxMatcher = this.trxPattern.matcher(line);
        if (trxMatcher.find()) {
            this.trxFile = trxMatcher.group(TRX_GROUP);
        }

        if (!this.attachmentsSection) {
            Matcher attachmentsMatcher = this.attachmentsPattern.matcher(line);

            if (attachmentsMatcher.matches()) {
                this.attachmentsSection = true;
            }
        } else {
            Matcher coverageMatcher = this.coveragePattern.matcher(line);
            if (coverageMatcher.find()) {
                this.coverageFile = coverageMatcher.group(COVERAGE_GROUP);
            }
        }

        out.write(bytes, 0, len);
    }

    @Override
    public void close() throws IOException {
        super.close();
        out.close();
    }
}
