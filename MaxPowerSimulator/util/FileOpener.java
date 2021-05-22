package util;/*
 * uk.ac.macaulay.util: util.FileOpener.java
 * 
 * Copyright (C) 2010 Macaulay Institute
 * 
 * This file is part of utils.
 * 
 * utils is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * utils is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with utils. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Author: Gary Polhill
 */

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

/**
 * <!-- util.FileOpener -->
 * 
 * @author Gary Polhill
 */
public class FileOpener {
  public static String[] URLstarters = new String[] { "http://", "https://", "ftp://", "file:/" };
  
  public static final String DEFAULT_IMAGE_FORMAT = "png";

  private FileOpener() {
    // Stop instances
  }

  public static BufferedReader read(String filename) throws IOException {
    for(int i = 0; i < URLstarters.length; i++) {
      if(filename.substring(0, URLstarters[i].length()).equals(URLstarters[i])) {
        return read(new URL(filename));
      }
    }
    return read(new FileInputStream(filename));
  }

  /**
   * <!-- read -->
   *
   * @param fileInputStream
   * @return
   */
  public static BufferedReader read(FileInputStream fileInputStream) throws IOException {
    return read(new InputStreamReader(fileInputStream));
  }

  /**
   * <!-- read -->
   *
   * @param inputStreamReader
   * @return
   */
  private static BufferedReader read(InputStreamReader inputStreamReader) throws IOException {
    return new BufferedReader(inputStreamReader);
  }

  public static BufferedReader read(URL url) throws IOException {
    return read(new InputStreamReader(url.openStream()));
  }

  public static BufferedReader read(URI uri) throws IOException {
    return read(uri.toURL());
  }

  public static BufferedReader read(File file) throws IOException {
    return read(new FileInputStream(file));
  }
  
  public static PrintWriter write(String filename) throws IOException {
    for(int i = 0; i < URLstarters.length; i++) {
      if(filename.substring(0, URLstarters[i].length()).equals(URLstarters[i])) {
        return write(new URL(filename));
      }
    }
    return write(new File(filename));
  }
  
  public static PrintWriter write(File file) throws IOException {
    return write(new FileWriter(file));
  }
  
  public static PrintWriter write(FileWriter fileWriter) throws IOException {
    return new PrintWriter(fileWriter);
  }
  
  public static PrintWriter write(URI uri) throws IOException {
    return write(uri.toURL());
  }
  
  public static PrintWriter write(URL url) throws IOException {
    return write(url.openConnection());
  }
  
  public static PrintWriter write(URLConnection connection) throws IOException {
    return write(connection.getOutputStream());
  }
  
  public static PrintWriter write(OutputStream stream) throws IOException {
    return new PrintWriter(stream);
  }
  
  public static BufferedImage readImage(String filename) throws IOException {
    for(int i = 0; i < URLstarters.length; i++) {
      if(filename.substring(0, URLstarters[i].length()).equals(URLstarters[i])) {
        return readImage(new URL(filename));
      }
    }
    return readImage(new File(filename));
  }
  
  public static BufferedImage readImage(URL url) throws IOException {
    return ImageIO.read(url);
  }
  
  public static BufferedImage readImage(URI uri) throws IOException {
    return readImage(uri.toURL());
  }
  
  public static BufferedImage readImage(File file) throws IOException {
    return readImage(new FileInputStream(file));
  }
  
  public static BufferedImage readImage(InputStream input) throws IOException {
    return ImageIO.read(input);
  }
  
  public static void writeImage(BufferedImage image, String filename) throws IOException {
    writeImage(image, DEFAULT_IMAGE_FORMAT, filename);
  }
  
  public static void writeImage(BufferedImage image, String format, String filename) throws IOException {
    for(int i = 0; i < URLstarters.length; i++) {
      if(filename.substring(0, URLstarters[i].length()).equals(URLstarters[i])) {
        writeImage(image, format, new URL(filename));
        return;
      }
    }
  
    writeImage(image, format, new File(filename));  
  }
  
  public static void writeImage(BufferedImage image, File file) throws IOException {
    writeImage(image, DEFAULT_IMAGE_FORMAT, file);
  }
  
  public static void writeImage(BufferedImage image, String format, File file) throws IOException {
    ImageIO.write(image, format, file);
  }
  
  public static void writeImage(BufferedImage image, URI uri) throws IOException {
    writeImage(image, DEFAULT_IMAGE_FORMAT, uri);
  }
  
  public static void writeImage(BufferedImage image, String format, URI uri) throws IOException {
    writeImage(image, format, uri.toURL());
  }
  
  public static void writeImage(BufferedImage image, URL url) throws IOException {
    writeImage(image, DEFAULT_IMAGE_FORMAT, url);
  }
  
  public static void writeImage(BufferedImage image, String format, URL url) throws IOException {
    writeImage(image, format, url.openConnection());
  }
  
  public static void writeImage(BufferedImage image, URLConnection connection) throws IOException {
    writeImage(image, DEFAULT_IMAGE_FORMAT, connection);
  }
  
  public static void writeImage(BufferedImage image, String format, URLConnection connection) throws IOException {
    writeImage(image, format, connection.getOutputStream());
  }
  
  public static void writeImage(BufferedImage image, OutputStream stream) throws IOException {
    writeImage(image, DEFAULT_IMAGE_FORMAT, stream);
  }
  
  public static void writeImage(BufferedImage image, String format, OutputStream stream) throws IOException {
    ImageIO.write(image, format, stream);
  }
}
