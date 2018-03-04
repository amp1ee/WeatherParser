import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

public class ProgramGUI {
    private static final String     slash;
    static final String             title;
    private static final String     fileExt;
    private static final String     listExt;
    private static final String     propFilename;
    private static JFrame           mainframe;
    private static JFileChooser     urlsChooser;
    private static JFileChooser     fileChooser;
    private static JButton          saveBtn;
    private static JButton          openBtn;
    private static String[]         dates;
    private static String[]         outputFiles;
    static JLabel                   curUrl;
    static JProgressBar             progressBar;

    static {
        title = "wParser";
        slash = File.separator;
        fileExt = ".json";
        listExt = ".lst";
        propFilename = System.getProperty("user.home") + slash + ".wparser";

    }

    private static void initJFrame() {
        Font    font;

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
            showErrMsg(e, " Error caused by UIManager.getSystemLookAndFeel()");
        }
        font = new Font("Sans-serif", Font.PLAIN, 18);
        progressBar = new JProgressBar();
        fileChooser = new JFileChooser();
        urlsChooser = new JFileChooser();
        openBtn = new JButton();
        saveBtn = new JButton();
        curUrl = new JLabel("");
        mainframe = new JFrame();

        mainframe.setSize(350, 100);
        mainframe.setResizable(false);
        mainframe.setLocationRelativeTo(null);
        mainframe.setTitle(title);
        mainframe.setLayout(new FlowLayout());

        fileChooser.setDialogTitle("Save to...");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter
                (new FileNameExtensionFilter(fileExt + " only", "lst"));

        urlsChooser.setDialogTitle("Choose URL's list");
        urlsChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        urlsChooser.setFileFilter
                (new FileNameExtensionFilter(listExt + " only", "json"));
        progressBar.setStringPainted(true);
        progressBar.setFont(font);
        progressBar.setPreferredSize(new Dimension(340, 40));
        progressBar.setForeground(Color.BLACK);

        curUrl.setVerticalAlignment(JLabel.BOTTOM);
        curUrl.setFont(new Font("Sans-serif", Font.BOLD, 14));
        curUrl.setVisible(true);

        mainframe.add(progressBar);
        mainframe.add(curUrl);
        mainframe.setVisible(true);
        mainframe.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private static boolean handleDialogs(String[] args) {
        final int   approve = JFileChooser.APPROVE_OPTION;
        int         amt;
        String      exportDir;
        File        jsonFile;
        File        optionsFile;
        String      defaultName;

        defaultName = title.replaceAll("\\s+","_") + "_" + dates[0] + fileExt;
        jsonFile = new File(defaultName);
        optionsFile = new File(propFilename);
        if (args.length != 0)
            amt = Integer.parseInt(args[0]);
        else
            amt = 8;
        amt = (amt > 0 && amt <= 8) ? amt : 8;
        dates = getDates(amt);
        if (optionsFile.exists()) {
            restoreDir(fileChooser, defaultName, urlsChooser);
        } else {
            fileChooser.setSelectedFile(jsonFile);
        }
        if (urlsChooser.showOpenDialog(openBtn) == approve &&
            fileChooser.showSaveDialog(saveBtn) == approve) {
            mainframe.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    try {
                        storeDir(fileChooser.getSelectedFile().getAbsolutePath(),
                                urlsChooser.getSelectedFile().getAbsolutePath());
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    sysExit(exitCodes.OK.ordinal());
                }
            });
            exportDir = fileChooser.getSelectedFile().getAbsolutePath();
            exportDir = exportDir.substring(0, exportDir.lastIndexOf(slash) + 1);
            outputFiles = getFileNames(exportDir, dates);
            return true;
        }
        else
            return false;
    }

    private static boolean parseWeather(WeatherParser wp) {
        boolean         success;
        String          urlsFile;

        urlsFile = urlsChooser.getSelectedFile().getAbsolutePath();
        success = wp.parse(outputFiles, urlsFile);
        if (wp.connections == wp.urls.size()) {
            mainframe.setTitle(mainframe.getTitle() + " - " +
                (success ? "Parsing finished" : "Finished with errors"));
            curUrl.setText(success ?
                "Done" : "Finished with errors (see log file)");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return success;
    }

    public static void showErrMsg(Exception e, String data) {
        final String    title = mainframe.getTitle() + " - " + e.getClass().getSimpleName();
        String          msg;

        msg = e.getMessage();
        if (msg != null && !(e.getCause() instanceof IllegalArgumentException))
            msg += ": " + data;
        else if (msg == null)
            msg = e.getClass().getSimpleName() + ": " + data;
        JOptionPane.showMessageDialog(mainframe, msg, title, JOptionPane.ERROR_MESSAGE);
    }

    private static void sysExit(int exitCode) {
        System.exit(exitCode);
    }

    private static void storeDir(String outPath, String inPath) {
        Properties p;

        outPath = outPath.substring(0, outPath.lastIndexOf(slash));
        p = new Properties();
        p.setProperty("lastDir", outPath + slash);
        p.setProperty("urlDir", inPath);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(propFilename))) {
            p.store(bw, "Last used folders for " + mainframe.getTitle());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void restoreDir(JFileChooser fileChooser, String outName, JFileChooser urlsChooser) {
        Properties  p;
        File        exportFile;
        File        urlsFile;

        p = new Properties();
        try (BufferedReader br = new BufferedReader(new FileReader(propFilename))) {
            p.load(br);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        exportFile = new File(p.getProperty("lastDir") + outName);
        urlsFile = new File(p.getProperty("urlDir"));
        fileChooser.setSelectedFile(exportFile);
        urlsChooser.setSelectedFile(urlsFile);
    }

    private static String[] getDates(int size) {
        String[]            datesArray;
        Date                date;
        SimpleDateFormat    sdf;
        Calendar            cal;

        datesArray = new String[size];
        date = new Date();
        sdf = new SimpleDateFormat("dd_MM_yyyy");
        cal = Calendar.getInstance();
        cal.setTime(date);
        for (int i = 0; i < size; i++) {
            cal.add(Calendar.DATE, 1); // Adding one day
            date = cal.getTime();
            datesArray[i] = sdf.format(date);
        }
        return datesArray;
    }

    private static String[] getFileNames(String exportDir, String[] dates) {
        final int   size = dates.length;
        String[]    names;

        names = new String[size];
        for (int i = 0; i < size; i++) {
            names[i] = exportDir.concat("weather_" + dates[i] + ".json");
        }
        return names;
    }

    private enum exitCodes {
        OK,
        ERROR
    }

    public static void main(String[] args) {
        initJFrame();
        if (!handleDialogs(args))
            sysExit(exitCodes.ERROR.ordinal());
        if (parseWeather(new WeatherParser()))
            mainframe.dispatchEvent(new WindowEvent(mainframe, WindowEvent.WINDOW_CLOSING));
    }
}