package org.jenkinsci.plugins.vstest_runner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hudson.console.LineTransformationOutputStream;
import hudson.model.TaskListener;

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

    private final OutputStream listener;

    private final Pattern trxPattern = Pattern.compile(TRX_PATTERN);
    private final Pattern attachmentsPattern = Pattern.compile(ATTACHMENTS_PATTERN);
    private final Pattern coveragePattern = Pattern.compile(COVERAGE_PATTERN);

    private boolean attachmentsSection;

    private String trxFile;
    private String coverageFile;

    public VsTestListenerDecorator(TaskListener listener) {
        this.listener = listener != null ? listener.getLogger() : null;
    }

    public String getTrxFile() {
        return trxFile;
    }

    public String getCoverageFile() {
        return coverageFile;
    }

    @Override
    protected void eol(byte[] bytes, int len) throws IOException {

        if (listener == null) {
            return;
        }

        String line = new String(bytes, 0, len, Charset.defaultCharset());

        Matcher trxMatcher = trxPattern.matcher(line);
        if (trxMatcher.find()) {
            trxFile = trxMatcher.group(TRX_GROUP);
        }

        if (!attachmentsSection) {
            Matcher attachmentsMatcher = attachmentsPattern.matcher(line);

            if (attachmentsMatcher.matches()) {
                attachmentsSection = true;
            }
        } else {
            Matcher coverageMatcher = coveragePattern.matcher(line);
            if (coverageMatcher.find()) {
                coverageFile = coverageMatcher.group(COVERAGE_GROUP);
            }
        }

        listener.write(line.getBytes(Charset.defaultCharset()));
    }

    @Override
    public void close() throws IOException {
        super.close();
        listener.close();
    }
}
