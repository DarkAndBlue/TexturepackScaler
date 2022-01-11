package texturepackscaler;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static javax.swing.text.DefaultCaret.ALWAYS_UPDATE;

public class TexturepackScaler extends JFrame {
  public static void main(String[] args) {
    new TexturepackScaler();
  }
  
  private static final int padding = 4;
  private static final Font font = new Font("Arial", Font.PLAIN, 18);
  private static final Integer[] resolutions = new Integer[] { 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192 };
  private static final SCALEMODE[] scaleModes = new SCALEMODE[] {
    SCALEMODE.BILINEAR, SCALEMODE.BICUBIC, SCALEMODE.NEAREST_NEIGHBOR, SCALEMODE.SCALE_AREA_AVERAGING,
    SCALEMODE.SCALE_FAST, SCALEMODE.SCALE_DEFAULT, SCALEMODE.SCALE_SMOOTH, SCALEMODE.SCALE_REPLICATE
  };
  
  private File scanPath = null;
  private JComboBox<Integer> searchComboBox;
  private JComboBox<Integer> convertComboBox;
  private JComboBox<SCALEMODE> scalingModeComboBox;
  private JLabel process;
  
  private Thread runningAsyncThread;
  
  public TexturepackScaler() {
    setLayout(null);
    setSize(500, 700);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setResizable(false);
    
    JTextArea pathTextArea = new JTextArea("path to scan in");
    pathTextArea.setLineWrap(true);
    pathTextArea.setEditable(false);
    pathTextArea.setOpaque(true);
    pathTextArea.setBackground(new Color(170, 170, 170));
    pathTextArea.setFont(font);
    pathTextArea.setLocation(padding, padding);
    pathTextArea.setSize(getWidth() - padding * 5, 80);
    add(pathTextArea);
    
    JButton pathButton = new JButton("eddit path");
    pathButton.setFont(font);
    pathButton.setSize(115, 40);
    pathButton.setLocation(getWidth() / 2 - pathButton.getWidth() / 2, pathTextArea.getY() + pathTextArea.getHeight() + padding);
    pathButton.addActionListener(e -> {
      JFileChooser fileChooser = new JFileChooser(new File(System.getenv("APPDATA") + "/.minecraft"));
      fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      int result = fileChooser.showOpenDialog(null);
      if (result == JFileChooser.APPROVE_OPTION) {
        scanPath = fileChooser.getSelectedFile();
        pathTextArea.setText("" + scanPath.getAbsolutePath());
      }
    });
    add(pathButton);
    
    
    searchComboBox = new JComboBox<>(resolutions);
    searchComboBox.setSize(115, 40);
    searchComboBox.setFont(font);
    searchComboBox.setLocation(getWidth() / 2 - searchComboBox.getWidth() / 2, pathButton.getY() + pathButton.getHeight() + padding);
    add(searchComboBox);
    
    JLabel searchResolutionLabel = new JLabel("search resolution");
    searchResolutionLabel.setFont(font);
    searchResolutionLabel.setSize(150, 40);
    searchResolutionLabel.setLocation(searchComboBox.getX() - searchResolutionLabel.getWidth(), pathButton.getY() + pathButton.getHeight() + padding);
    add(searchResolutionLabel);
    
    
    convertComboBox = new JComboBox<>(resolutions);
    convertComboBox.setSize(115, 40);
    convertComboBox.setFont(font);
    convertComboBox.setLocation(getWidth() / 2 - convertComboBox.getWidth() / 2, searchComboBox.getY() + searchComboBox.getHeight() + padding);
    add(convertComboBox);
    
    JLabel convertResolutionLabel = new JLabel("convert resolution");
    convertResolutionLabel.setFont(font);
    convertResolutionLabel.setSize(150, 40);
    convertResolutionLabel.setLocation(convertComboBox.getX() - convertResolutionLabel.getWidth(), searchComboBox.getY() + searchComboBox.getHeight() + padding);
    add(convertResolutionLabel);
    
    
    scalingModeComboBox = new JComboBox<>(scaleModes);
    scalingModeComboBox.setSize(160, 40);
    scalingModeComboBox.setLocation(getWidth() / 2 - scalingModeComboBox.getWidth() / 2, convertComboBox.getY() + convertComboBox.getHeight() + padding);
    add(scalingModeComboBox);
    
    
    JButton startButton = new JButton("Start");
    startButton.setFont(font);
    startButton.setSize(115, 40);
    startButton.setLocation(getWidth() / 2 - startButton.getWidth() / 2, scalingModeComboBox.getY() + scalingModeComboBox.getHeight() + padding);
    startButton.addActionListener(e -> {
      if(runningAsyncThread != null)
        runningAsyncThread.interrupt();
  
      runningAsyncThread = new Thread(()->{
        startScan();
      });
  
      runningAsyncThread.start();
    });
    add(startButton);
    
    process = new JLabel("wating for start");
    process.setFont(font);
    process.setSize(150, 40);
    process.setLocation(getWidth() / 2 - process.getWidth() / 2, startButton.getY() + startButton.getHeight() + padding);
    add(process);
    
    
    JTextArea consoleTextArea = new JTextArea();
    DefaultCaret caret = (DefaultCaret) consoleTextArea.getCaret();
    caret.setUpdatePolicy(ALWAYS_UPDATE);
    consoleTextArea.setLineWrap(true);
    consoleTextArea.setEditable(false);
    consoleTextArea.setOpaque(true);
    consoleTextArea.setBackground(new Color(170, 170, 170));
    consoleTextArea.setFont(new Font(font.getName(), font.getStyle(), 14));
    JScrollPane scrollPane = new JScrollPane(consoleTextArea);
    scrollPane.setSize(getWidth() - padding * 5, getHeight() - process.getY() - process.getHeight() - 45);
    scrollPane.setLocation(padding, process.getY() + process.getHeight() + padding);
    add(scrollPane);
    PrintStream printStream = new PrintStream(new CustomOutputStream(consoleTextArea));
    System.setOut(printStream);
//    System.setErr(printStream);
//    
    System.out.println("Console");
    
    setVisible(true);
    setLocationRelativeTo(null);
    
    JOptionPane.showMessageDialog(null, "Node: every image in your selcted path with the selected search resolution will be overritten, so make a backup of your files first befor start converting.");
  }
  
  private void startScan() {
    if (scanPath == null) {
      JOptionPane.showMessageDialog(null, "please select a directory first");
      return;
    }
    process.setText("still converting...");
    
    List<File> fileList = getAllFiles();
    
    for (File file : fileList) {
      if (file.getName().endsWith(".png") || file.getName().endsWith(".PNG")) {
        try {
          BufferedImage image = ImageIO.read(file);
          
          if (image != null) {
            int height = image.getHeight(null);
            int width = image.getWidth(null);
            
            int searchResolution = (Integer) searchComboBox.getSelectedItem();
            
            if (height == searchResolution && width == searchResolution) {
              downScaleAndOverrideImage(file, image);
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    
    process.setText("finished converting");
    Toolkit.getDefaultToolkit().beep();
  }
  
  private void downScaleAndOverrideImage(File file, BufferedImage image) {
    int convertResolution = (Integer) convertComboBox.getSelectedItem();
    
    SCALEMODE scalemode = (SCALEMODE) scalingModeComboBox.getSelectedItem();
    
//    int imageType = image.getType();
//    if(imageType == 0) 
    int imageType = BufferedImage.TYPE_INT_ARGB;
    BufferedImage newImage;
    
    if(scalemode.value instanceof IMAGESCALING) {
      Image toolkitImage = image.getScaledInstance(convertResolution, convertResolution, ((IMAGESCALING) scalemode.value).value);

      newImage = new BufferedImage(convertResolution, convertResolution, imageType);
      Graphics graphics = newImage.getGraphics();
      graphics.drawImage(toolkitImage, 0, 0, null);
      graphics.dispose();
    } else {
      BufferedImage bufferedImage = new BufferedImage(convertResolution, convertResolution, imageType);
      Graphics2D graphics2D = bufferedImage.createGraphics();
      graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,scalemode.value);
      graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
      graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
      graphics2D.drawImage(image, 0, 0, convertResolution, convertResolution, null);
      graphics2D.dispose();
      
      newImage = bufferedImage;
    }
    
    try {
      ImageIO.write(newImage, "png", file);
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println("changed file: " + file.getName());
  }
  
  private List<File> getAllFiles() {
    return getAllFiles(scanPath, new ArrayList<>());
  }
  
  private List<File> getAllFiles(File path, List<File> fileList) {
    File[] fileArray = path.listFiles();
    if (fileArray != null) {
      for (File file : fileArray) {
        if (file.isFile()) {
          fileList.add(file);
        } else if (file.isDirectory()) {
          getAllFiles(new File(file.getAbsolutePath()), fileList);
        }
      }
    }
    
    return fileList;
  }
  
  class CustomOutputStream extends OutputStream {
    private JTextArea textArea;
    
    public CustomOutputStream(JTextArea textArea) {
      this.textArea = textArea;
    }
    
    @Override
    public void write(int b) {
      textArea.append(String.valueOf((char) b));
      textArea.setCaretPosition(textArea.getDocument().getLength());
    }
  }
  
  enum SCALEMODE {
    BILINEAR(RenderingHints.VALUE_INTERPOLATION_BILINEAR),
    BICUBIC(RenderingHints.VALUE_INTERPOLATION_BICUBIC),
    NEAREST_NEIGHBOR(RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR),
    SCALE_AREA_AVERAGING(IMAGESCALING.SCALE_AREA_AVERAGING),
    SCALE_FAST(IMAGESCALING.SCALE_FAST),
    SCALE_DEFAULT(IMAGESCALING.SCALE_DEFAULT),
    SCALE_SMOOTH(IMAGESCALING.SCALE_SMOOTH),
    SCALE_REPLICATE(IMAGESCALING.SCALE_REPLICATE);
    
    private final Object value;
    
    SCALEMODE(Object value) {
      this.value = value;
    }
  }
  
  enum IMAGESCALING {
    SCALE_AREA_AVERAGING(Image.SCALE_AREA_AVERAGING),
    SCALE_FAST(Image.SCALE_FAST),
    SCALE_DEFAULT(Image.SCALE_DEFAULT),
    SCALE_SMOOTH(Image.SCALE_SMOOTH),
    SCALE_REPLICATE(Image.SCALE_REPLICATE);
  
    private final int value;
  
    IMAGESCALING(int value) {
      this.value = value;
    }
  }
}