/* Copyright (C) 2006-2010  Joan Queralt Molina
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package biogenesis;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.*;
import java.util.List;


/**
 * This class implements a full organism's genetic code. A genetic code is
 * composed by a number of genes, a symmetry, optional mirroring, optional children
 * dispersing, stress resistance, and energy needed to reproduce it.
 * 
 * Genes are represented with segments that form the organism's body and are
 * drawn one behind the other. This basic set is repeated symmetry times in one
 * of two possible ways:
 * 
 * If mirroring is not applied, each repetition is rotated so they are distributed
 * in a uniform way around the central (starting) point.
 * 
 * When mirrored, segments are calculated as follows:
 * First, third, fifth and septh repetitions are applied a rotation of
 * 0, 180, 90, 270 degrees respectively.
 * 
 * Second repetition is like the first one with opposite x coordinate.
 * Fourth repetition is like the third one with opposite x coordinate.
 * Sixth repetition is like the fifth one with opposite y coordinate.
 * Eigth repetition is like the septh one with opposite y coordinate.
 */
public class GeneticCode implements Cloneable, Serializable {
	/** The version of this class */
	private static final long serialVersionUID = Utils.FILE_VERSION;
	/**
	 * X-Mod scaffolding (Slice 1): segment/symmetry bounds and mutation
	 * dispatch sizes. Constants only — copy-ctor still uses vanilla logic
	 * until Slice 2 lands.
	 */
	protected static final int MAX_SEGMENTS = 64;
	protected static final int MIN_SEGMENTS = 4;
	protected static final int MAX_SYMMETRY = 8;
	protected static final int MIN_SYMMETRY = 1;
	protected static final int N_MAJOR_MUTATIONS = 10;
	protected static final int N_MINOR_MUTATIONS = 4;
	/**
	 * X-Mod: cached shortest/longest gene length. Transient — recomputed via
	 * update(), never serialized. Serial streams require matching FILE_VERSION.
	 */
	protected transient double minCurrentGeneLength = 0d;
	protected transient double maxCurrentGeneLength = 0d;
	/**
	 * Array with the genes. Every gene is represented by symmetry
	 * segments when drawing the organism.
	 */
	protected Gene[] _genes;
	/**
	 * The symmetry used when drawing the organism. Possible values are
	 * 1 - 8. 
	 */
	protected int _symmetry;
	/**
	 * Mirroring indicates if symmetric segments are drawn in the same
	 * way than the original, only changing their angle, or if they are
	 * drawn like in a mirror. 0 = not mirrored, 1 = mirrored
	 */
	protected int _mirror;
	/**
	 * Indicates if children will be placed moving towards the same
	 * direction than the father or to a different direction to move
	 * away.
	 */
	protected boolean _disperseChildren;
	/**
	 * Minimum required energy to reproduce this genetic code.
	 * More genes, means more energy is needed. 
	 */
	protected int _reproduceEnergy;
	// Getters
	/**
	 * Returns the symmetry applied to organisms with this genetic code
	 * 
	 * @return  a value of 1 - 8.
	 */
	public int getSymmetry() {
		return _symmetry;
	}
	/**
	 * Returns if mirroring is applied to organisms with this genetic code
	 * 
	 * @return  0 if no mirroring is applied, 1 if mirroring is applied.
	 */
	public int getMirror() {
		return _mirror;
	}
	/**
	 * Returns if organisms with this genetic code will disperse their children or not.
	 * 
	 * @return  true if the organism disperses its children, false otherwise.
	 */
	public boolean getDisperseChildren() {
		return _disperseChildren;
	}
	/**
	 * Returns the energy needed to replicate this genetic code.
	 * This energy is equal to 40 plus 3 for each segment.
	 * 
	 * @return  the energy needed to replicate this genetic code.
	 */
	public int getReproduceEnergy() {
		return _reproduceEnergy;
	}
	/**
	 * Return a reference to a gene.
	 * 
	 * @param i  The index of the gene in the genetic code.
	 * @return  A reference to the gene
	 */
	public Gene getGene(int i) {
		return _genes[i];
	}
	/** Return the number of genes of this code
	 * */
	public int getNGenes() {
		return _genes.length;
	}
	/** Gives mirror a random value (0 or 1) */
	private void randomMirror() {
		_mirror = Utils.random.nextInt(2);
	}
	/** Gives symmetry a random value (2, 4 or 8) */
	private void randomSymmetry() {
		_symmetry = Utils.random.nextInt(8)+1;
	}
	/**
	 * Create a random genes array making sure that there will be more or equal than
	 * 1 and less or equal than 8.
	 * It needs symmetry to have a valid value. 
	 */
	private void randomGenes() {
		int nSegments = (1 + Utils.random.nextInt(8)) * _symmetry; // 1 - 8
		if (nSegments % _symmetry != 0)
		    nSegments += (_symmetry - (nSegments % _symmetry));
		int nGenes = nSegments / _symmetry;
		_genes = new Gene[nGenes];
		for (int i=0; i<nGenes; i++) {
			_genes[i] = new Gene();
			_genes[i].randomize();
		}
	}
	/**
	 * Decide randomly if organisms with this genetic code will try to
	 * disperse their children or not.
	 */
	private void randomDisperseChildren() {
		_disperseChildren =  Utils.random.nextBoolean();
	}
	/**
	 * Calculates the energy required to reproduce this genetic code.
	 * This energy is 40 plus 3 for each segment.
	 */
	private void calculateReproduceEnergy() {
		_reproduceEnergy = 40 + 3 * _genes.length * _symmetry;
	}
	/**
	 * X-Mod Slice 1: recompute derived state (energy + length limits).
	 * Not wired into copy-ctor yet.
	 */
	private void update() {
		calculateReproduceEnergy();
		calculateCurrentGeneLengthLimits();
	}
	/** X-Mod Slice 1: cache shortest/longest gene length. */
	private void calculateCurrentGeneLengthLimits() {
		minCurrentGeneLength = Double.MAX_VALUE;
		maxCurrentGeneLength = 0d;
		if (_genes == null)
			return;
		for (int i = 0; i < _genes.length; i++) {
			if (_genes[i] == null)
				continue;
			if (_genes[i].getLength() < minCurrentGeneLength)
				minCurrentGeneLength = _genes[i].getLength();
			if (_genes[i].getLength() > maxCurrentGeneLength)
				maxCurrentGeneLength = _genes[i].getLength();
		}
		if (minCurrentGeneLength == Double.MAX_VALUE)
			minCurrentGeneLength = 0d;
	}
	/**
	 * X-Mod Slice 1: total length of genes with the given color.
	 * Zero-guard added (X-Mod divides unguarded elsewhere).
	 */
	protected double calculateTotalLength(Color color) {
		double total = 0d;
		if (_genes == null)
			return 0d;
		for (int i = 0; i < _genes.length; i++) {
			if (_genes[i] != null && color.equals(_genes[i].getColor()))
				total += _genes[i].getLength();
		}
		return total;
	}
	/** X-Mod Slice 1: average length of genes with the given color. */
	protected double calculateAverageLength(Color color) {
		double total = 0d;
		int n = 0;
		if (_genes == null)
			return 0d;
		for (int i = 0; i < _genes.length; i++) {
			if (_genes[i] != null && color.equals(_genes[i].getColor())) {
				total += _genes[i].getLength();
				n++;
			}
		}
		if (n == 0)
			return 0d;
		return total / n;
	}
	/**
	 * X-Mod Slice 1: Green (1995) RJMCMC acceptance ratio for split moves.
	 * (x,y) is the split intersection in the segment-centered frame.
	 */
	protected static double splitSegmentsAcceptanceRatio(double l, double x, double y) {
		double p = 2 * x + l;
		double q = 2 * x - l;
		return 2d * l * (2 * Gene.MAX_LENGTH - l) / (Math.PI * (Gene.MAX_LENGTH - Gene.MIN_LENGTH))
				* Math.sqrt((2 * Gene.MAX_LENGTH - l) * (2 * Gene.MAX_LENGTH + l)
						/ ((p * p + 4 * y * y) * (q * q + 4 * y * y)));
	}
	/** Creates a new random genetic code. */
	public GeneticCode() {
		randomMirror(); 
		randomSymmetry();
		randomGenes();
		randomDisperseChildren();
		calculateReproduceEnergy();
		calculateCurrentGeneLengthLimits();
	}	
	/**
	 * Creates a genetic code given its content.
	 * No check about the validity of the information is done.
	 * 
	 * @param genes  A list containing the genes
	 * @param symmetry  The symmetry that an organism with this genetic code will have.
	 * @param mirror  0 if the organism won't be mirrored, 1 if it will.
	 * @param disperseChildren  true if the organism will disperse its children.
	 */
	public GeneticCode(List<Gene> genes, int symmetry, int mirror, boolean disperseChildren) {
		int nGenes = genes.size();
		_genes = new Gene[nGenes];
		genes.toArray(_genes);
		_mirror = mirror;
		_symmetry = symmetry;
		_disperseChildren = disperseChildren;
		calculateReproduceEnergy();
		calculateCurrentGeneLengthLimits();
	}
	/**
	 * Recompute transient caches after Java deserialization.
	 * Requires matching serialVersionUID (FILE_VERSION).
	 */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		calculateCurrentGeneLengthLimits();
	}
	/**
	 * Creates a new genetic code based on the father genetic code but
	 * applying random mutations to it.
	 * 
	 * @param parentCode  The genetic code that this code will be based on.
	 */
	public GeneticCode(GeneticCode parentCode) {
		// X-Mod: initialize with parent code, clone all genes upfront
		// (fixes children-sharing-same-Gene-instances synchronous mutation bug).
		_mirror = parentCode.getMirror();
		_symmetry = parentCode.getSymmetry();
		_genes = new Gene[parentCode.getNGenes()];
		for (int i = 0; i < parentCode.getNGenes(); i++) {
			_genes[i] = parentCode.getGene(i).clone();
		}
		_disperseChildren = parentCode.getDisperseChildren();

		// Check repeatedly for mutations — produces Poisson-distributed
		// run of mutations per reproduction (X-Mod detailed balance).
		while (Utils.checkReproductionMutation()) {
			if (Utils.minorOrMajorMutation()) {
				applyMajorMutation();
			} else {
				applyMinorMutation();
			}
		}

		update();
	}

	/** X-Mod: alter symmetry by +-1 if segment counts stay valid. */
	private void alterSymmetry() {
		int newSymmetry = _symmetry + 2 * Utils.random.nextInt(2) - 1;
		if (newSymmetry >= MIN_SYMMETRY && newSymmetry <= MAX_SYMMETRY
				&& newSymmetry * getNGenes() >= MIN_SEGMENTS
				&& newSymmetry * getNGenes() <= MAX_SEGMENTS) {
			_symmetry = newSymmetry;
		}
	}

	/**
	 * X-Mod: mutate spontaneously (no reproduction needed).
	 * @return true if mutated.
	 */
	public boolean applySpontaneousMutations() {
		boolean mutated = false;
		while (Utils.checkSpontaneousMutation()) {
			mutated = splitOrMergeSegments(true) || mutated;
		}
		if (mutated) {
			calculateReproduceEnergy();
			calculateCurrentGeneLengthLimits();
		}
		return mutated;
	}

	private boolean applyMajorMutation() {
		boolean mutated = false;
		switch (Utils.random.nextInt(N_MAJOR_MUTATIONS)) {
			case 0:
				mutated = true;
				randomMirror();
				break;
			case 1:
				mutated = true;
				randomDisperseChildren();
				break;
			case 2:
				mutated = true;
				alterSymmetry();
				break;
			case 3:
				mutated = addOrRemoveSegments() || mutated;
				break;
			case 4:
				mutated = splitOrMergeSegments(true) || mutated;
				break;
			case 5:
				mutated = flipGeneSequence() || mutated;
				break;
			case 6:
				if (getNGenes() > 1)
					mutated = swapSegments(Utils.random.nextInt(getNGenes() - 1)) || mutated;
				break;
			case 7: {
				mutated = true;
				int i = Utils.random.nextInt(getNGenes());
				_genes[i].randomizeLength();
				calculateCurrentGeneLengthLimits();
				break;
			}
			case 8:
				mutated = true;
				_genes[Utils.random.nextInt(getNGenes())].randomizeTheta();
				break;
			case 9:
				mutated = true;
				_genes[Utils.random.nextInt(getNGenes())].randomizeColor();
				break;
			default:
				break;
		}
		update();
		return mutated;
	}

	private boolean addOrRemoveSegments() {
		boolean mutated = false;
		Gene insertedGene;
		int i;
		if (Utils.random.nextBoolean()) {
			if ((getNGenes() + 1) * getSymmetry() <= MAX_SEGMENTS) {
				mutated = true;
				insertedGene = new Gene();
				insertedGene.randomize();
				i = Utils.random.nextInt(getNGenes() + 1);
				insertGene(i, insertedGene);
				if (i + 1 < getNGenes())
					_genes[i + 1].setTheta(_genes[i + 1].getTheta() - _genes[i].getTheta());
				update();
			}
		} else {
			if ((getNGenes() - 1) * getSymmetry() >= MIN_SEGMENTS) {
				mutated = true;
				i = Utils.random.nextInt(getNGenes());
				if (i + 1 < getNGenes())
					_genes[i + 1].setTheta(_genes[i + 1].getTheta() + _genes[i].getTheta());
				removeGene(i);
				update();
			}
		}
		return mutated;
	}

	private boolean swapSegments(int segment) {
		Gene newSeg1, newSeg2, seg1, seg2;
		if (segment + 1 >= getNGenes() || segment < 0)
			return false;
		seg1 = _genes[segment];
		seg2 = _genes[segment + 1];
		newSeg1 = new Gene(seg2.getLength(), seg1.getTheta() + seg2.getTheta(), seg2.getColor());
		newSeg2 = new Gene(seg1.getLength(), 2 * Math.PI - seg2.getTheta(), seg1.getColor());
		_genes[segment] = newSeg1;
		_genes[segment + 1] = newSeg2;
		if (segment + 2 < getNGenes())
			_genes[segment + 2].setTheta(_genes[segment + 2].getTheta() + seg2.getTheta());
		return true;
	}

	private boolean flipGeneSequence() {
		double[][] segmentPos;
		int i, j, k;
		double thetaAxis, theta1, theta2, dx, dy;
		if (getNGenes() > 1) {
			i = Utils.random.nextInt(getNGenes());
			j = Utils.random.nextInt(getNGenes() - 1);
			if (j >= i)
				j++;
			else {
				k = j;
				j = i;
				i = k;
			}
		} else {
			return false;
		}
		segmentPos = createSegmentPositions();
		if (i == 0) {
			dx = segmentPos[j][0];
			dy = segmentPos[j][1];
		} else {
			dx = segmentPos[j][0] - segmentPos[i - 1][0];
			dy = segmentPos[j][1] - segmentPos[i - 1][1];
		}
		thetaAxis = Math.atan2(dy, dx);
		if (i == 0) {
			theta1 = 0.0;
		} else if (i == 1) {
			theta1 = _genes[0].getTheta();
		} else {
			theta1 = Math.atan2(segmentPos[i - 1][1] - segmentPos[i - 2][1],
					segmentPos[i - 1][0] - segmentPos[i - 2][0]);
		}
		theta2 = Math.atan2(segmentPos[j][1] - segmentPos[j - 1][1],
				segmentPos[j][0] - segmentPos[j - 1][0]);
		_genes[i].setTheta(2 * (thetaAxis - theta1) - _genes[i].getTheta());
		for (k = i + 1; k <= j; k++) {
			_genes[k].setTheta(2.0 * Math.PI - _genes[k].getTheta());
		}
		if (j + 1 < getNGenes())
			_genes[j + 1].setTheta(_genes[j + 1].getTheta() - 2 * (thetaAxis - theta2));
		return true;
	}

	private double[][] createSegmentPositions() {
		double[][] xy = new double[getNGenes()][2];
		double xc = 0.0, yc = 0.0, thetac = 0.0;
		for (int i = 0; i < getNGenes(); i++) {
			thetac = thetac + _genes[i].getTheta();
			xc = xc + Math.cos(thetac) * _genes[i].getLength();
			yc = yc + Math.sin(thetac) * _genes[i].getLength();
			xy[i][0] = xc;
			xy[i][1] = yc;
		}
		return xy;
	}

	private void insertGene(int pos, Gene insertedGene) {
		int nGenes = getNGenes();
		Gene[] newGenes = new Gene[nGenes + 1];
		for (int i = 0, j = 0; j <= nGenes; j++) {
			if (j != pos) {
				newGenes[j] = _genes[i];
				i++;
			} else {
				newGenes[j] = insertedGene;
			}
		}
		_genes = newGenes;
	}

	private void removeGene(int pos) {
		int nGenes = getNGenes();
		Gene[] newGenes = new Gene[nGenes - 1];
		for (int i = 0, j = 0; i < nGenes; i++) {
			if (i != pos) {
				newGenes[j] = _genes[i];
				j++;
			}
		}
		_genes = newGenes;
	}

	private boolean applyMinorMutation() {
		boolean mutated = false;
		switch (Utils.random.nextInt(N_MINOR_MUTATIONS)) {
			case 0:
				mutated = true;
				_genes[Utils.random.nextInt(getNGenes())].alterTheta();
				break;
			case 1:
				mutated = true;
				_genes[Utils.random.nextInt(getNGenes())].alterLength();
				calculateCurrentGeneLengthLimits();
				break;
			case 2: {
				double scalingFactor = Utils.minorMutationFactor();
				if (scalingFactor * minCurrentGeneLength > Gene.MIN_LENGTH
						&& scalingFactor * maxCurrentGeneLength < Gene.MAX_LENGTH) {
					mutated = true;
					for (int i = 0; i < getNGenes(); i++) {
						_genes[i].setLength(scalingFactor * _genes[i].getLength());
					}
					minCurrentGeneLength = minCurrentGeneLength * scalingFactor;
					maxCurrentGeneLength = maxCurrentGeneLength * scalingFactor;
				}
				break;
			}
			case 3:
				if (splitOrMergeSegments(false)) {
					mutated = true;
					calculateCurrentGeneLengthLimits();
				}
				break;
			default:
				break;
		}
		return mutated;
	}

	private boolean splitOrMergeSegments(boolean randomizeColor) {
		boolean mutated = false;
		int i;
		if (Utils.random.nextBoolean()) {
			if ((getNGenes() + 1) * getSymmetry() <= MAX_SEGMENTS) {
				mutated = splitSegment(Utils.random.nextInt(getNGenes()), randomizeColor);
			}
		} else {
			if ((getNGenes() - 1) * getSymmetry() >= MIN_SEGMENTS) {
				i = Utils.random.nextInt(getNGenes() - 1);
				if (!randomizeColor && !_genes[i].getColor().equals(_genes[i + 1].getColor()))
					return false;
				mutated = mergeSegments(i);
			}
		}
		if (mutated)
			update();
		return mutated;
	}

	private boolean splitSegment(int segment, boolean randomizeColor) {
		Gene oldGene = _genes[segment].clone();
		Gene newGene1, newGene2;
		double length = oldGene.getLength();
		double x, y, probAcc, l1, l2, theta1, theta2, theta3 = 0;
		x = (Utils.random.nextDouble() * 2 - 1) * (Gene.MAX_LENGTH - 0.5 * length);
		y = (Utils.random.nextDouble() * 2 - 1) * Math.sqrt(Gene.MAX_LENGTH * Gene.MAX_LENGTH - 0.25 * length * length);
		l1 = Math.sqrt((0.5 * length + x) * (0.5 * length + x) + y * y);
		l2 = Math.sqrt((0.5 * length - x) * (0.5 * length - x) + y * y);
		if (l1 > Gene.MAX_LENGTH || l1 < Gene.MIN_LENGTH
				|| l2 > Gene.MAX_LENGTH || l2 < Gene.MIN_LENGTH)
			return false;
		probAcc = splitSegmentsAcceptanceRatio(length, x, y);
		if (probAcc < 1 && Utils.random.nextDouble() > probAcc)
			return false;
		theta1 = _genes[segment].getTheta() + Math.atan2(y, 0.5 * length + x);
		theta2 = Math.atan2(0.5 * length + x, y) + Math.atan2(0.5 * length - x, y) - Math.PI;
		if (segment + 1 < getNGenes())
			theta3 = _genes[segment + 1].getTheta() + Math.atan2(y, 0.5 * length - x);
		newGene1 = new Gene(l1, theta1, oldGene.getColor());
		newGene2 = new Gene(l2, theta2, oldGene.getColor());
		if (randomizeColor) {
			if (Utils.random.nextDouble() * (l1 + l2) < l1) {
				newGene2.randomizeColor();
			} else {
				newGene1.randomizeColor();
			}
		}
		_genes[segment] = newGene1;
		insertGene(segment + 1, newGene2);
		if (segment + 2 < getNGenes())
			_genes[segment + 2].setTheta(theta3);
		return true;
	}

	private boolean mergeSegments(int segment) {
		double length, alpha, x, y, probAcc;
		Color color;
		if (segment + 1 >= getNGenes())
			return false;
		length = Math.sqrt(Math.pow(_genes[segment].getLength(), 2)
				+ Math.pow(_genes[segment + 1].getLength(), 2)
				- 2 * _genes[segment].getLength() * _genes[segment + 1].getLength()
				* Math.cos(Math.PI - _genes[segment + 1].getTheta()));
		if (length < Gene.MIN_LENGTH || length > Gene.MAX_LENGTH)
			return false;
		alpha = Math.atan2(Math.sin(_genes[segment + 1].getTheta()) * _genes[segment + 1].getLength(),
				_genes[segment].getLength() + Math.cos(_genes[segment + 1].getTheta()) * _genes[segment + 1].getLength());
		x = Math.cos(alpha) * _genes[segment].getLength() - 0.5 * length;
		y = Math.sin(alpha) * _genes[segment].getLength();
		probAcc = 1d / splitSegmentsAcceptanceRatio(length, x, y);
		if (probAcc < 1 && Utils.random.nextDouble() > probAcc)
			return false;
		if (getNGenes() > segment + 2) {
			_genes[segment + 2].setTheta(_genes[segment + 2].getTheta() + _genes[segment + 1].getTheta() - alpha);
		}
		if (Utils.random.nextDouble() < 1 / (1 + _genes[segment + 1].getLength() / _genes[segment].getLength())) {
			color = _genes[segment].getColor();
		} else {
			color = _genes[segment + 1].getColor();
		}
		_genes[segment] = new Gene(length, _genes[segment].getTheta() + alpha, color);
		removeGene(segment + 1);
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() {
		GeneticCode newCode = null;
		try {
			newCode = (GeneticCode) super.clone();
			newCode._genes = new Gene[_genes.length];
			for (int i=0; i<_genes.length; i++)
				newCode._genes[i] = (Gene) _genes[i].clone();
		} catch (CloneNotSupportedException e) {// We should never reach this
		}
		return newCode;
	}
	
	/**
	 * Draws a representation of this genetic code. This representation
	 * is equivalent to draw an adult organism with this genetic code and
	 * no rotation.
	 * 
	 * @param g  The place where the representation is drawn.
	 * @param width  The width of the available space. 
	 * @param height  The height of the available space.
	 */
	public void draw(Graphics g, int width, int height) {
		int[][] x0 = new int[_symmetry][_genes.length];
		int[][] y0 = new int[_symmetry][_genes.length];
		int[][] x1 = new int[_symmetry][_genes.length];
		int[][] y1 = new int[_symmetry][_genes.length];
		int maxX = 0;
		int minX = 0;
		int maxY = 0;
		int minY = 0;
		double scale = 1.0;
		Vector2D v = new Vector2D();
		Graphics2D g2 = (Graphics2D) g;

		for (int i=0; i<_symmetry; i++) {
			for (int j=0; j<_genes.length; j++) {
				v.setModulus(_genes[j].getLength());
				if (j==0) {
					x0[i][j]=y0[i][j]=0;
					if (_mirror == 0 || i%2==0)
						v.setTheta(_genes[j].getTheta()+i*2*Math.PI/_symmetry);
					else {
						v.setTheta(_genes[j].getTheta()+(i-1)*2*Math.PI/_symmetry);
						v.invertX();
					}
				} else {
					x0[i][j] = x1[i][j-1];
					y0[i][j] = y1[i][j-1];
					if (_mirror == 0 || i%2==0)
						v.addDegree(_genes[j].getTheta());
					else
						v.addDegree(-_genes[j].getTheta());
				}
				
				x1[i][j] = (int) Math.round(v.getX() + x0[i][j]);
				y1[i][j] = (int) Math.round(v.getY() + y0[i][j]);
				
				maxX = Math.max(maxX, Math.max(x0[i][j], x1[i][j]));
				maxY = Math.max(maxY, Math.max(y0[i][j], y1[i][j]));
				minX = Math.min(minX, Math.min(x0[i][j], x1[i][j]));
				minY = Math.min(minY, Math.min(y0[i][j], y1[i][j]));
			}
		}
		
		g2.translate(width/2, height/2);
		if (maxX-minX > width)
			scale = (double)width/(double)(maxX-minX);
		if (maxY-minY > height)
			scale = Math.min(scale, (double)height/(double)(maxY-minY));
		g2.scale(scale, scale);
		
		for (int i=0; i<_symmetry; i++) {
			for (int j=0; j<_genes.length; j++) {
				x0[i][j]+=(-minX-maxX)/2;
				x1[i][j]+=(-minX-maxX)/2;
				y0[i][j]+=(-minY-maxY)/2;
				y1[i][j]+=(-minY-maxY)/2;
				g2.setColor(_genes[j].getColor());
				g2.drawLine(x0[i][j],y0[i][j],x1[i][j],y1[i][j]);
			}
		}
	}
}

