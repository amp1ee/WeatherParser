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
    public static JLabel cururl;
    public static JProgressBar progress = new JProgressBar();
    private static JFrame frame;
    private static final String propFilename = System.getProperty("user.home") + "/w2json_dir.prop";

    private static void storeDir(String path) {
        Properties p = new Properties();
        path = path.substring(0, path.lastIndexOf(File.separator)); //path without fileName
        System.out.println(path);
        p.setProperty("lastDir", path + "\\");

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(propFilename))){
            p.store(bw, "Last used folder for Weather2JSONParser");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private static void restoreDir(JFileChooser fc, String name) {
        Properties p = new Properties();
        try (BufferedReader br = new BufferedReader(new FileReader(propFilename))) {
            p.load(br);
        } catch (IOException e) {
            e.printStackTrace();
        }
        File f = new File(p.getProperty("lastDir") + name);
        fc.setSelectedFile(f);
    }

    public static void main(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        JFileChooser fc = new JFileChooser();
        JButton save = new JButton();
        fc.setDialogTitle("Save as...");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(new FileNameExtensionFilter("JSON only", "json"));

        Date dt = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(dt);
        c.add(Calendar.DATE, 1); //tomorrow date
        dt = c.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_yyyy");
        String tomorrow = sdf.format(dt);

        String defaultName = "weather_"+tomorrow+".json";

        File f = new File(defaultName);

        File optionsFile = new File(propFilename);
        if (optionsFile.exists()) {
            restoreDir(fc, defaultName);
        } else {
            fc.setSelectedFile(f);
        }

        if (fc.showSaveDialog(save) == JFileChooser.APPROVE_OPTION) {
            //
            Font font = new Font("Sans-serif", Font.PLAIN, 18);
            frame = new JFrame();
            frame.setSize(350,100);
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setTitle("Parsing weather-forecast.com...");
            frame.setLayout(new FlowLayout());

            frame.addWindowListener( new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    try {
                        storeDir(fc.getSelectedFile().getAbsolutePath());
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

            cururl = new JLabel("");
            cururl.setVerticalAlignment(JLabel.BOTTOM);
            cururl.setFont(new Font("Sans-serif", Font.BOLD, 14));
            cururl.setVisible(true);

            frame.add(progress);
            frame.add(cururl);
            frame.setVisible(true);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            //frame.pack();

        } else {
            System.exit(0);
        }

        WeatherParser wp = new WeatherParser();
        try {
            wp.parse(fc.getSelectedFile().getAbsolutePath());

            if (wp.connections >= wp.urls.size()) {
                frame.setTitle("Success");
                //progress.setVisible(false);
                //progress.setEnabled(false);

                cururl.setText("Parsing finished!");

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
