package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class SeamsCarver extends ImageProcessor {

	// MARK: An inner interface for functional programming.
	@FunctionalInterface
	interface ResizeOperation {
		BufferedImage resize();
	}

	// MARK: Fields
	private int numOfSeams;
	private ResizeOperation resizeOp;
	boolean[][] imageMask;

	long[][] costArr;
	long[][] energyArr;
	int[][] indicesArr;
	BufferedImage greyImage;
	int[] currentSeam;
	int updatedWidth;
	int[][] grey_matrix;
	int[][] seamsArray;
	int seamsArrayIndex;

	public SeamsCarver(Logger logger, BufferedImage workingImage, int outWidth, RGBWeights rgbWeights,
			boolean[][] imageMask) {
		super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, workingImage.getHeight());

		numOfSeams = Math.abs(outWidth - inWidth);
		this.imageMask = imageMask;
		if (inWidth < 2 | inHeight < 2)
			throw new RuntimeException("Can not apply seam carving: workingImage is too small");

		if (numOfSeams > inWidth / 2)
			throw new RuntimeException("Can not apply seam carving: too many seams...");

		// Setting resizeOp by with the appropriate method reference
		if (outWidth > inWidth)
			resizeOp = this::increaseImageWidth;
		else if (outWidth < inWidth)
			resizeOp = this::reduceImageWidth;
		else
			resizeOp = this::duplicateWorkingImage;

		updatedWidth = workingImage.getWidth();
		costArr = new long[inHeight][inWidth];
		energyArr = new long[workingImage.getHeight()][updatedWidth];
		currentSeam = new int[inHeight];
		seamsArray = new int[inHeight][numOfSeams];
		seamsArrayIndex = 0;
		indicesArr = new int[inHeight][inWidth];
		initIndicesArr();
		this.greyImage = greyscale();
		updateGreyMatrix();
		updatepPictureWithMask();
		this.logger.log(Integer.toString(updatedWidth));
		this.logger.log("preliminary calculations were ended.");
		this.grey_matrix = new int[inHeight][inWidth];
	}

	/**
	 * 
	 */
	private void initIndicesArr() {
		for (int i = 0; i < inHeight; i++) {
			for (int j = 0; j < inWidth; j++) {
				indicesArr[i][j] = j;
			}
		}
	}

	/**
	 * 
	 */
	private void updateGreyMatrix() {
		this.grey_matrix = new int[inHeight][updatedWidth];
		setForEachParameters(updatedWidth, inHeight);

		forEach((y, x) -> {
			int x_index = indicesArr[y][x];
			grey_matrix[y][x] = greyImage.getRGB(x_index, y);
		});

	}

	/**
	 * calculates the the energy table.
	 */
	private void calculateEnergyArr() {
		setForEachParameters(updatedWidth, inHeight);
		forEach((y, x) -> {
			int dx = 0;
			int dy = 0;
			if (x == 0) {
				dx = grey_matrix[y][x];
			} else {
				dx = grey_matrix[y][x] - grey_matrix[y][x - 1];
			}

			if (y == 0) {
				dy = grey_matrix[y][x];
			} else {
				dy = grey_matrix[y][x] - grey_matrix[y - 1][x];
			}
			energyArr[y][x] = Math.abs(dx) + Math.abs(dy);
		});
	}

	/**
	 * 
	 */
	private void calcCostArr() {
		setForEachParameters(updatedWidth, inHeight);
		forEach((y, x) -> {
			if (y == 0) {
				costArr[y][x] = energyArr[y][x];
			} else if (x == 0 && y != 0) {
				costArr[y][x] = energyArr[y][x]
						+ Math.min(costArr[y - 1][x] + cU(x, y), costArr[y - 1][x + 1] + cR(x, y));
			} else if (x == updatedWidth - 1 && y != 0) {
				costArr[y][x] = energyArr[y][x]
						+ Math.min(costArr[y - 1][x] + cU(x, y), costArr[y - 1][x - 1] + cL(x, y));
			} else {
				costArr[y][x] = energyArr[y][x] + Math.min(costArr[y - 1][x] + cU(x, y),
						Math.min(costArr[y - 1][x - 1] + cL(x, y), costArr[y - 1][x + 1] + cR(x, y)));
			}
		});
	}

	/**
	 * 
	 * @param x - width loc
	 * @param y - height loc
	 * @return
	 */
	private long cL(int x, int y) {
		if (x == 0) {
			return Math.abs(grey_matrix[y][x + 1] + grey_matrix[y - 1][x]);
		} else if (x == updatedWidth - 1) {
			return Math.abs(grey_matrix[y][x - 1]) + Math.abs((grey_matrix[y - 1][x]) - grey_matrix[y][x - 1]);
		} else {
			return Math.abs(grey_matrix[y][x + 1] - grey_matrix[y][x - 1])
					+ Math.abs(grey_matrix[y - 1][x] - grey_matrix[y][x - 1]);
		}
	}

	/**
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	private long cR(int x, int y) {
		if (x == 0) {
			return Math.abs(grey_matrix[y][x + 1]) + Math.abs(grey_matrix[y - 1][x] - grey_matrix[y][x + 1]);
		} else if (x == updatedWidth - 1) {
			return Math.abs(grey_matrix[y][x - 1]) + Math.abs(grey_matrix[y - 1][x]);
		} else {
			return Math.abs(grey_matrix[y][x + 1] - grey_matrix[y][x - 1])
					+ Math.abs(grey_matrix[y - 1][x] - grey_matrix[y][x + 1]);
		}
	}

	/**
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	private long cU(int x, int y) {
		if (x == 0) {
			return Math.abs(grey_matrix[y][x + 1]);
		} else if (x == updatedWidth - 1) {
			return Math.abs(grey_matrix[y][x - 1]);
		} else {
			return Math.abs(grey_matrix[y][x + 1] - grey_matrix[y][x - 1]);
		}
	}

	/**
	 * 
	 */
	private void findSeam() {
		int currentIndex;
		currentSeam[inHeight - 1] = findBottomMinCostPixel();
		for (int i = inHeight - 2; i >= 0; i--) {
			if (currentSeam[i + 1] == updatedWidth - 1) {
				if (Math.min(costArr[i][updatedWidth - 1],
						costArr[i][updatedWidth - 2]) == costArr[i][updatedWidth - 1]) {
					currentSeam[i] = updatedWidth - 1;
				} else {
					currentSeam[i] = updatedWidth - 2;
				}
			} else if (currentSeam[i + 1] == 0) {
				if (Math.min(costArr[i][0], costArr[i][1]) == costArr[i][0]) {
					currentSeam[i] = 0;
				} else {
					currentSeam[i] = 1;
				}
			} else {
				currentIndex = currentSeam[i + 1];
				if (costArr[i][currentIndex] < costArr[i][currentIndex - 1]
						&& costArr[i][currentIndex] < costArr[i][currentIndex + 1]) {
					currentSeam[i] = currentIndex;
				} else if (costArr[i][currentIndex - 1] < costArr[i][currentIndex]
						&& costArr[i][currentIndex - 1] < costArr[i][currentIndex + 1]) {
					currentSeam[i] = currentIndex - 1;
				} else {
					currentSeam[i] = currentIndex + 1;
				}

			}

		}

		// adding the seam to the seamAarray

		for (int i = 0; i < inHeight; i++) {
			seamsArray[i][seamsArrayIndex] = currentSeam[i];
		}
		seamsArrayIndex++;

	}

	/**
	 * 
	 * @param seamArr
	 */
	private void removeSeam(int[] seamArr) {
		setForEachParameters(updatedWidth - 1, workingImage.getHeight());

		forEach((y, x) -> {
			if (x < seamArr[y]) {
				indicesArr[y][x] = indicesArr[y][x];
			} else {
				indicesArr[y][x] = indicesArr[y][x + 1];
			}
		});

	}

	/**
	 * 
	 * @return
	 */
	private int findBottomMinCostPixel() {
		int minIndex = 0;
		long minCost = Long.MAX_VALUE;

		for (int i = 0; i < updatedWidth; i++) {
			if (costArr[inHeight - 1][i] < minCost) {
				minIndex = i;
				minCost = costArr[inHeight - 1][i];
			}
		}
		return minIndex;
	}

	public BufferedImage resize() {
		return resizeOp.resize();
	}

	private void updatepPictureWithMask() {
		setForEachParameters(inWidth, inHeight);
		forEach((y, x) -> {
			if (imageMask[y][x] == true) {
				grey_matrix[y][x] = Integer.MAX_VALUE;
			}
		});

	}

	/**
	 * 
	 * @return
	 */
	private BufferedImage reduceImageWidth() {

		while (updatedWidth > inWidth - numOfSeams) {
			calculateEnergyArr();
			calcCostArr();
			findSeam();
			removeSeam(currentSeam);
			updatedWidth--;
			updateGreyMatrix();
		}

		BufferedImage ans = newEmptyImage(updatedWidth, inHeight);

		setForEachParameters(updatedWidth, inHeight);
		forEach((y, x) -> {
			ans.setRGB(x, y, workingImage.getRGB(indicesArr[y][x], y));
		});

		return ans.getSubimage(0, 0, outWidth, outHeight);
	}

	/**
	 * 
	 * @return
	 */
	private BufferedImage increaseImageWidth() {

		// removing the seams from the picture
		while (updatedWidth > inWidth - numOfSeams) {
			calculateEnergyArr();
			calcCostArr();
			findSeam();
			removeSeam(currentSeam);
			updatedWidth--;
			updateGreyMatrix();
		}

		BufferedImage ans = newEmptyOutputSizedImage();

		// replicate seams
		for (int i = 0; i < inHeight; i++) {
			int largeImageIndex = 0;
			int indeicesArrayIndex = 0;
			for (int k = 0; k < inWidth; k++) {
				if (indicesArr[i][indeicesArrayIndex] > k) {
					ans.setRGB(largeImageIndex, i, workingImage.getRGB(k, i));
					ans.setRGB(largeImageIndex + 1, i, workingImage.getRGB(k, i));
					largeImageIndex = largeImageIndex + 2;
				} else {
					ans.setRGB(largeImageIndex, i, workingImage.getRGB(k, i));
					largeImageIndex = largeImageIndex + 1;
					indeicesArrayIndex++;
				}
			}

		}

		return ans;
	}

	/**
	 * 
	 * @param seamColorRGB
	 * @return
	 */
	public BufferedImage showSeams(int seamColorRGB) {

		while (updatedWidth > inWidth - numOfSeams) {
			calculateEnergyArr();
			calcCostArr();
			findSeam();
			removeSeam(currentSeam);
			updatedWidth--;
			updateGreyMatrix();
		}

		BufferedImage ans = duplicateWorkingImage();
		Color red = new Color(255, 0, 0);

		for (int i = 0; i < inHeight; i++) {
			for (int j = 0; j < numOfSeams; j++) {
				for (int k = 0; k < inWidth; k++) {
					if (seamsArray[i][j] == k) {
						ans.setRGB(k, i, red.getRGB());
					}
				}
			}
		}

		return ans;
	}

	/**
	 * 
	 * @return
	 */
	public boolean[][] getMaskAfterSeamCarving() {
		boolean[][] MaskMatrix = new boolean[inHeight][outWidth];

		// new mask matrix when reducing
		if (outWidth < inWidth) {
			reduceImageWidth();
			setForEachParameters(outWidth, inHeight);
			forEach((y, x) -> {
				MaskMatrix[y][x] = imageMask[y][indicesArr[y][x]];
			});

		}

		// new mask matrix when increasing
		if (outWidth > inWidth) {
			increaseImageWidth();
			setForEachParameters(outWidth, inHeight);
			for (int i = 0; i < inHeight; i++) {
				int newMaskIndex = 0;
				int indeicesArrayIndex = 0;
				for (int k = 0; k < inWidth; k++) {
					if (indicesArr[i][indeicesArrayIndex] > k) {
						MaskMatrix[i][newMaskIndex] = imageMask[i][indicesArr[i][k]];
						MaskMatrix[i][newMaskIndex + 1] = imageMask[i][indicesArr[i][k]];
						newMaskIndex = newMaskIndex + 2;
					} else {
						MaskMatrix[i][newMaskIndex] = imageMask[i][indicesArr[i][k]];
						newMaskIndex = newMaskIndex + 1;
						indeicesArrayIndex++;
					}
				}

			}

		}

		return MaskMatrix;
	}
}
