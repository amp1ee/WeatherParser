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

public class FileSaveDialog  {
    static JLabel curUrl;
    static JProgressBar progress = new JProgressBar();
    private static String slash = File.separator;
    private static JFrame mainframe;
    private static final String propFilename = System.getProperty("user.home") + slash + ".wparser";

    private static void storeDir(String outPath, String inPath) {
        Properties p = new Properties();
        outPath = outPath.substring(0, outPath.lastIndexOf(slash)); //path without fileName
        p.setProperty("lastDir", outPath + slash);
        p.setProperty("urlDir", inPath);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(propFilename))){
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

    public static void main(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        JFileChooser fc = new JFileChooser();
        JButton save = new JButton();
        fc.setDialogTitle("Save as...");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(new FileNameExtensionFilter("JSON only", "json"));

        JFileChooser urlsChooser = new JFileChooser();
        JButton open = new JButton();
        urlsChooser.setDialogTitle("Choose weather URLs list");
        urlsChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        urlsChooser.setFileFilter(new FileNameExtensionFilter(".lst only", "lst"));

        int amt;
        if (args.length != 0)
            amt = Integer.parseInt(args[0]);
        else
            amt = 8;
        amt = (amt > 0 && amt <= 8) ? amt : 8;
        String[] dates = getDates(amt);
        String defaultName = "weather_"+ dates[0] +".json";
        File f = new File(defaultName);
        File optionsFile = new File(propFilename);
        if (optionsFile.exists()) {
            restoreDir(fc, defaultName, urlsChooser);
        } else {
            fc.setSelectedFile(f);
        }

        if (urlsChooser.showOpenDialog(open) == JFileChooser.APPROVE_OPTION &&
                fc.showSaveDialog(save) == JFileChooser.APPROVE_OPTION) {
            //
            Font font = new Font("Sans-serif", Font.PLAIN, 18);
            mainframe = new JFrame();
            mainframe.setSize(350,100);
            mainframe.setResizable(false);
            mainframe.setLocationRelativeTo(null);
            mainframe.setTitle("Parsing weather-forecast.com...");
            mainframe.setLayout(new FlowLayout());

            mainframe.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    try {
                        storeDir(fc.getSelectedFile().getAbsolutePath(),
                                urlsChooser.getSelectedFile().getAbsolutePath());
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                    System.exit(0);
                }
            });

            progress.setStringPainted(true);
            progress.setFont(font);
            progress.setPreferredSize( new Dimension (340, 40));
            progress.setForeground(Color.BLACK);

            curUrl = new JLabel("");
            curUrl.setVerticalAlignment(JLabel.BOTTOM);
            curUrl.setFont(new Font("Sans-serif", Font.BOLD, 14));
            curUrl.setVisible(true);

            mainframe.add(progress);
            mainframe.add(curUrl);
            mainframe.setVisible(true);
            mainframe.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        } else {
            System.exit(0);
        }
        String exportDir = fc.getSelectedFile().getAbsolutePath();
        exportDir = exportDir.substring(0, exportDir.lastIndexOf(slash) + 1);
        String[] files = getFileNames(exportDir, dates);
        WeatherParser wp = new WeatherParser();
        try {
            boolean failed = wp.parse(files, mainframe,
                    urlsChooser.getSelectedFile().getAbsolutePath());
            if (wp.connections >= wp.urls.size()) {
                mainframe.setTitle(failed ? "Finished with errors" : "Success");
                curUrl.setText(failed ? "Finished with errors (see log file)." : "Parsing finished successfully!");

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!failed)
                    mainframe.dispatchEvent(new WindowEvent(mainframe, WindowEvent.WINDOW_CLOSING));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static String[] getDates(int amt) {
        String[] dates = new String[amt];
        Date dt = new Date();
        Calendar c = Calendar.getInstance();

        c.setTime(dt);
        for (int i = 0; i < amt; i++) { //tomorrow, after tomorrow, and so on
            c.add(Calendar.DATE, 1); //next date
            dt = c.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_yyyy");
            dates[i] = sdf.format(dt);
        }
        return dates;
    }

    private static String[] getFileNames(String exportDir, String[] dates) {
        int amt = dates.length;
        String[] names = new String[amt];
        for (int i = 0; i < amt; i++) {
            names[i] = exportDir.concat("weather_"+ dates[i] +".json");
        }
        return names;
    }
}
