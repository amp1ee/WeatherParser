import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class FileSaveDialog  {
    static WeatherParser wp;
    static JProgressBar progress;
    static JLabel cururl;
    static JFrame frame;
    static Font font;


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

        fc.setSelectedFile(new File(defaultName));

        if (fc.showSaveDialog(save) == JFileChooser.APPROVE_OPTION) {
            //
            font = new Font("Sans-serif", Font.PLAIN, 18);
            frame = new JFrame();
            frame.setSize(350,100);
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setTitle("Parsing weather-forecast.com...");
            frame.setLayout(new FlowLayout());

            progress = new JProgressBar();
            progress.setStringPainted(true);
            progress.setFont(font);
            progress.setPreferredSize( new Dimension (340, 40));
            progress.setForeground(Color.BLACK);

            cururl = new JLabel("");
            cururl.setVerticalAlignment(JLabel.BOTTOM);
            cururl.setFont(new Font("Sans-serif", Font.BOLD, 14));
            cururl.setVisible(true);
            //progress.setIndeterminate(true);
            frame.add(progress);
            frame.add(cururl);
            frame.setVisible(true);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            //frame.pack();

        } else {
            System.exit(0);
        }

        wp = new WeatherParser();
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
