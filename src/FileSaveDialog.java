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

public class FileSaveDialog {
    private static JFrame mainframe;
    private static JFileChooser urlsChooser;
    private static JFileChooser fileChooser;
    private static JButton saveBtn;
    private static JButton openBtn;
    static JLabel curUrl;
    static JProgressBar progress;
    private static final String slash = File.separator;
    private static final String propFilename = System.getProperty("user.home") + slash + ".wparser";
    private static final Font font = new Font("Sans-serif", Font.PLAIN, 18);
    private static String[] dates;
    private static String[] files;

    private static void storeDir(String outPath, String inPath) {
        Properties p = new Properties();
        outPath = outPath.substring(0, outPath.lastIndexOf(slash)); //path without fileName
        p.setProperty("lastDir", outPath + slash);
        p.setProperty("urlDir", inPath);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(propFilename))) {
            p.store(bw, "Last used folders for WParser");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void restoreDir(JFileChooser fc, String outName, JFileChooser urlsChooser) {
        Properties p = new Properties();
        try (BufferedReader br = new BufferedReader(new FileReader(propFilename))) {
            p.load(br);
        } catch (IOException e) {
            e.printStackTrace();
        }
        File f = new File(p.getProperty("lastDir") + outName);
        File f2 = new File(p.getProperty("urlDir"));
        fc.setSelectedFile(f);
        urlsChooser.setSelectedFile(f2);
    }


    private static void initJFrame(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException,
            InstantiationException, IllegalAccessException {
        File jsonFile;
        File optionsFile;
        String defaultName;
        int amt;

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        progress = new JProgressBar();
        fileChooser = new JFileChooser();
        saveBtn = new JButton();
        fileChooser.setDialogTitle("Save as...");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON only", "json"));

        urlsChooser = new JFileChooser();
        openBtn = new JButton();
        urlsChooser.setDialogTitle("Choose weather URLs list");
        urlsChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        urlsChooser.setFileFilter(new FileNameExtensionFilter(".lst only", "lst"));
        if (args.length != 0)
            amt = Integer.parseInt(args[0]);
        else
            amt = 8;
        amt = (amt > 0 && amt <= 8) ? amt : 8;
        dates = getDates(amt);
        defaultName = "weather_" + dates[0] + ".json";
        jsonFile = new File(defaultName);
        optionsFile = new File(propFilename);
        if (optionsFile.exists()) {
            restoreDir(fileChooser, defaultName, urlsChooser);
        } else {
            fileChooser.setSelectedFile(jsonFile);
        }
    }

    private static boolean handleDialogs() {
        if (urlsChooser.showOpenDialog(openBtn) == JFileChooser.APPROVE_OPTION &&
                fileChooser.showSaveDialog(saveBtn) == JFileChooser.APPROVE_OPTION) {
            mainframe = new JFrame();
            mainframe.setSize(350, 100);
            mainframe.setResizable(false);
            mainframe.setLocationRelativeTo(null);
            mainframe.setTitle("wParser 1.5");
            mainframe.setLayout(new FlowLayout());

            mainframe.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    try {
                        storeDir(fileChooser.getSelectedFile().getAbsolutePath(),
                                urlsChooser.getSelectedFile().getAbsolutePath());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.exit(0);
                }
            });
            progress.setStringPainted(true);
            progress.setFont(font);
            progress.setPreferredSize(new Dimension(340, 40));
            progress.setForeground(Color.BLACK);

            curUrl = new JLabel("");
            curUrl.setVerticalAlignment(JLabel.BOTTOM);
            curUrl.setFont(new Font("Sans-serif", Font.BOLD, 14));
            curUrl.setVisible(true);

            mainframe.add(progress);
            mainframe.add(curUrl);
            mainframe.setVisible(true);
            mainframe.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            String exportDir = fileChooser.getSelectedFile().getAbsolutePath();
            exportDir = exportDir.substring(0, exportDir.lastIndexOf(slash) + 1);
            files = getFileNames(exportDir, dates);
            return true;
        } else
            return false;
    }

    private static boolean parseWeather() {
        boolean success = false;
        WeatherParser wp = new WeatherParser();
        try {
            success = wp.parse(files, mainframe,
                    urlsChooser.getSelectedFile().getAbsolutePath());
            if (wp.connections >= wp.urls.size()) {
                mainframe.setTitle(!(success) ? "Finished with errors" : "Success");
                curUrl.setText(!(success) ? "Finished with errors (see log file)." : "Parsing finished");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return success;
    }

    private static String[] getDates(int amt) {
        String[] dates = new String[amt];
        Date dt = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_yyyy");
        Calendar c = Calendar.getInstance();

        c.setTime(dt);
        for (int i = 0; i < amt; i++) { //tomorrow, after tomorrow, and so on
            c.add(Calendar.DATE, 1); //next date
            dt = c.getTime();
            dates[i] = sdf.format(dt);
        }
        return dates;
    }

    private static String[] getFileNames(String exportDir, String[] dates) {
        int amt = dates.length;
        String[] names = new String[amt];
        for (int i = 0; i < amt; i++) {
            names[i] = exportDir.concat("weather_" + dates[i] + ".json");
        }
        return names;
    }

    public static void main(String[] args) {
        try {
            initJFrame(args);
        } catch (Exception e) {
            e.getCause();
        }
        if (!handleDialogs())
            System.exit(0);
        if (parseWeather())
            mainframe.dispatchEvent(new WindowEvent(mainframe, WindowEvent.WINDOW_CLOSING));
    }
}
