package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ImageProcessor extends FunctioalForEachLoops {

	// MARK: fields
	public final Logger logger;
	public final BufferedImage workingImage;
	public final RGBWeights rgbWeights;
	public final int inWidth;
	public final int inHeight;
	public final int workingImageType;
	public final int outWidth;
	public final int outHeight;

	// MARK: constructors
	public ImageProcessor(Logger logger, BufferedImage workingImage, RGBWeights rgbWeights, int outWidth,
			int outHeight) {
		super(); // initializing for each loops...

		this.logger = logger;
		this.workingImage = workingImage;
		this.rgbWeights = rgbWeights;
		inWidth = workingImage.getWidth();
		inHeight = workingImage.getHeight();
		workingImageType = workingImage.getType();
		this.outWidth = outWidth;
		this.outHeight = outHeight;
		setForEachInputParameters();
	}

	public ImageProcessor(Logger logger, BufferedImage workingImage, RGBWeights rgbWeights) {
		this(logger, workingImage, rgbWeights, workingImage.getWidth(), workingImage.getHeight());
	}

	// MARK: change picture hue - example
	public BufferedImage changeHue() {
		logger.log("Prepareing for hue changing...");

		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int max = rgbWeights.maxWeight;

		BufferedImage ans = newEmptyInputSizedImage();

		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int red = r * c.getRed() / max;
			int green = g * c.getGreen() / max;
			int blue = b * c.getBlue() / max;
			Color color = new Color(red, green, blue);
			ans.setRGB(x, y, color.getRGB());
		});

		logger.log("Changing hue done!");

		return ans;
	}

	public final void setForEachInputParameters() {
		setForEachParameters(inWidth, inHeight);
	}

	public final void setForEachOutputParameters() {
		setForEachParameters(outWidth, outHeight);
	}

	public final BufferedImage newEmptyInputSizedImage() {
		return newEmptyImage(inWidth, inHeight);
	}

	public final BufferedImage newEmptyOutputSizedImage() {
		return newEmptyImage(outWidth, outHeight);
	}

	public final BufferedImage newEmptyImage(int width, int height) {
		return new BufferedImage(width, height, workingImageType);
	}

	// A helper method that deep copies the current working image.
	public final BufferedImage duplicateWorkingImage() {
		BufferedImage output = newEmptyInputSizedImage();
		setForEachInputParameters();
		forEach((y, x) -> output.setRGB(x, y, workingImage.getRGB(x, y)));
		return output;
	}

	public BufferedImage greyscale() {
		BufferedImage output = newEmptyInputSizedImage();
		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int amount = rgbWeights.weightsAmount;
		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int red = c.getRed() * r;
			int green = c.getGreen() * g;
			int blue = c.getBlue() * b;
			int grey_scale = (red + green + blue) / amount;
			Color grey = new Color(grey_scale, grey_scale, grey_scale);
			output.setRGB(x, y, grey.getRGB());

		});

		return output;
	}

	/**
	 * nearest neighbor algorithm for resizing images
	 * 
	 * @return the resized output image after implementing NN
	 */
	public BufferedImage nearestNeighbor() {
		logger.log("Prepareing for resizing image with nearest neighbor...");

		// calculating x,y ratios
		double xRatio = (double) inWidth / (double) outWidth;
		double yRatio = (double) inHeight / (double) outHeight;

		BufferedImage ans = newEmptyOutputSizedImage();
		setForEachOutputParameters();

		forEach((y, x) -> {
			double xPixel = xRatio * x;
			double yPixel = yRatio * y;

			// creating a new color using the new xPixel,yPixel (coordinates)
			// and setting it to the returned image
			Color color = new Color(workingImage.getRGB((int) xPixel, (int) yPixel));
			ans.setRGB(x, y, color.getRGB());
		});

		logger.log("resizing image with nearest neighbor, done!");
		return ans;
	}
}
