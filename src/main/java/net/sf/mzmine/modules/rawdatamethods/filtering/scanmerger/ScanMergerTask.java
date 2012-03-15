package net.sf.mzmine.modules.rawdatamethods.filtering.scanmerger;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Merges scans from multiple raw data files into a single file.
 *
 * @author $Author$
 * @version $Revision$
 */
public class ScanMergerTask extends AbstractTask {

    // Logger.
    private static final Logger LOG = Logger.getLogger(ScanMergerParameters.class.getName());

    private final RawDataFile[] origDataFiles;
    private RawDataFile resultsDataFile;
    private int progressMax;
    private int progress;
    private final Boolean removeOriginals;
    private final String fileName;

    /**
     * Create the task.
     *
     * @param parameters task parameters.
     */
    public ScanMergerTask(final ParameterSet parameters) {

        // Initialize.
        resultsDataFile = null;
        progressMax = 0;
        progress = 0;

        // Get parameters.
        origDataFiles = parameters.getParameter(ScanMergerParameters.DATA_FILES).getValue();
        fileName = parameters.getParameter(ScanMergerParameters.FILE_NAME).getValue();
        removeOriginals = parameters.getParameter(ScanMergerParameters.REMOVE_ORIGINALS).getValue();
    }

    @Override
    public String getTaskDescription() {
        return "Merging raw data files";
    }

    @Override
    public double getFinishedPercentage() {
        return progressMax == 0 ? 0.0 : (double) progress / (double) progressMax;
    }

    @Override
    public Object[] getCreatedObjects() {
        return new Object[]{resultsDataFile};
    }

    @Override
    public void run() {

        // Update the status.
        setStatus(TaskStatus.PROCESSING);

        try {

            // Merge the scans.
            mergeScans();

            // If this task was canceled, stop processing.
            if (!isCanceled()) {

                // Add the newly created file to the project.
                final MZmineProject project = MZmineCore.getCurrentProject();
                project.addFile(resultsDataFile);

                // Remove the original data files if requested.
                if (removeOriginals) {

                    for (final RawDataFile file : origDataFiles) {
                        project.removeFile(file);
                    }
                }

                // Update status.
                setStatus(TaskStatus.FINISHED);

                LOG.info("Merged scans");
            }
        }
        catch (Throwable t) {

            LOG.log(Level.SEVERE, "Scan merger error", t);
            setStatus(TaskStatus.ERROR);
            errorMessage = t.getMessage();
        }
    }

    private void mergeScans()
            throws IOException {

        // Sort scans.
        final List<Scan> scanList = sortScans();

        progress = 0;
        progressMax = scanList.size() * 2;

        // Map scan numbers.
        final Map<RawDataFile, Map<Integer, Integer>> scanNumberMap = mapScanNumbers(scanList);

        // Create a new file.
        final RawDataFileWriter writer = MZmineCore.createNewFile(fileName);

        // Write remapped copies of scans.
        if (!isCanceled()) {
            for (final Scan scan : scanList) {
                writer.addScan(copyAndRemapScan(scan, scanNumberMap));
                progress++;
            }

            // Finalize writing
            resultsDataFile = writer.finishWriting();
        }
    }

    /**
     * Create a map between old and new scan numbers.
     *
     * @param scanList the list of scans.
     * @return the map.
     */
    private Map<RawDataFile, Map<Integer, Integer>> mapScanNumbers(final Iterable<Scan> scanList) {

        final Map<RawDataFile, Map<Integer, Integer>> scanNumberMap =
                new HashMap<RawDataFile, Map<Integer, Integer>>(origDataFiles.length);

        if (!isCanceled()) {
            int newScanNumber = 1;
            for (final Scan scan : scanList) {

                final RawDataFile file = scan.getDataFile();
                if (!scanNumberMap.containsKey(file)) {

                    scanNumberMap.put(file, new HashMap<Integer, Integer>(file.getNumOfScans()));
                }

                scanNumberMap.get(file).put(scan.getScanNumber(), newScanNumber++);
                progress++;
            }
        }

        return scanNumberMap;
    }

    /**
     * Sort all of the scans by RT and MS-level.
     *
     * @return a sorted list of all the scans.
     */
    private List<Scan> sortScans() {

        // Count scans.
        int scanCount = 0;
        for (final RawDataFile file : origDataFiles) {
            scanCount += file.getNumOfScans();
        }

        // Get all scans.
        final List<Scan> scanList = new ArrayList<Scan>(scanCount);
        if (!isCanceled()) {

            for (final RawDataFile file : origDataFiles) {
                for (final int scanNumber : file.getScanNumbers()) {
                    scanList.add(file.getScan(scanNumber));
                }
            }
        }

        // Sort on RT then MS-level.
        if (!isCanceled()) {

            Collections.sort(scanList, new Comparator<Scan>() {
                @Override
                public int compare(final Scan o1, final Scan o2) {
                    final int result = Double.compare(o1.getRetentionTime(), o2.getRetentionTime());
                    return result == 0 ? ((Integer) o1.getMSLevel()).compareTo(o2.getMSLevel()) : result;
                }
            });
        }

        return scanList;
    }

    /**
     * Copy a scan and remap its scan numbers (self, parent, fragments).
     *
     * @param scan the scan
     * @param map  the scan numbers map.
     * @return the remapped copy of the scan.
     */
    private static Scan copyAndRemapScan(final Scan scan, final Map<RawDataFile, Map<Integer, Integer>> map) {

        // Get data points (m/z and intensity pairs) of the original scan
        final DataPoint[] dataPoints = scan.getDataPoints();
        final DataPoint[] newDataPoints = new DataPoint[dataPoints.length];

        // Copy original data points.
        int i = 0;
        for (final DataPoint dp : dataPoints) {
            newDataPoints[i++] = new SimpleDataPoint(dp);
        }

        // Remap scan numbers.
        final RawDataFile dataFile = scan.getDataFile();
        final Map<Integer, Integer> scanNumberMap = map.get(dataFile);
        final int scanNumber = scanNumberMap.get(scan.getScanNumber());
        final int parentScanNumber = remapScanNumber(scanNumberMap, scan.getParentScanNumber());
        final int[] scanNumbers = scan.getFragmentScanNumbers();
        final int[] fragmentScanNumbers = scanNumbers == null ? null : new int[scanNumbers.length];
        if (fragmentScanNumbers != null) {
            int j = 0;
            for (final int fragmentScanNumber : scanNumbers) {
                fragmentScanNumbers[j++] = remapScanNumber(scanNumberMap, fragmentScanNumber);
            }
        }

        // Create and return new copied scan.
        return new SimpleScan(dataFile,
                              scanNumber,
                              scan.getMSLevel(),
                              scan.getRetentionTime(),
                              parentScanNumber,
                              scan.getPrecursorMZ(),
                              scan.getPrecursorCharge(),
                              fragmentScanNumbers,
                              newDataPoints,
                              scan.isCentroided());
    }

    /**
     * Lookup the scan number in the map, returning 0 if it's not found.
     *
     * @param map    the map to search.
     * @param number the number to lookup.
     * @return the value of the scan number in the map, or 0 if it's not found.
     */
    private static int remapScanNumber(final Map<Integer, Integer> map, final int number) {

        return map.containsKey(number) ? map.get(number) : 0;
    }
}
