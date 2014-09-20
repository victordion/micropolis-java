package micropolisj.graphics;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import javax.xml.stream.*;

import micropolisj.engine.*;
import static micropolisj.XML_Helper.*;

public abstract class TileImage
{
	public static final int STD_SIZE = 16;

	public static class DrawContext
	{
		public int time;
		public Micropolis city;
		public CityLocation location;
	}

	interface MultiPart
	{
		MultiPart makeEmptyCopy();
		Iterable<? extends Part> parts();
		void addPartLike(TileImage m, Part p);
		TileImage asTileImage();
	}

	interface Part
	{
		TileImage getImage();
	}

	public static class RealImage
	{
		public final TileImage image;
		public final TileCondition condition;
		//public final AnimatedTime animationTime;

		public RealImage(TileCondition condition, TileImage image)
		{
			this.image = image;
			this.condition = condition;
		}
	}

	/**
	 * Draws a part of this tile image to the given graphics context.
	 */
	public abstract void drawFragment(Graphics2D gr, int srcX, int srcY, int srcWidth, int srcHeight);

	public final void drawTo(Graphics2D gr, int destX, int destY)
	{
		final Dimension sz = getSize();

		Graphics2D g1 = (Graphics2D) gr.create();
		g1.translate(destX, destY);
		this.drawFragment(g1, 0, 0, sz.width, sz.height);
	}

	/**
	 * @return the total width and height needed for this tile image.
	 */
	public abstract Dimension getBounds();

	/**
	 * Get the standard size of this tile image. This is the distance between
	 * adjacent tiles. It is usually also the size of the image itself,
	 * but is sometimes smaller than the actual image since the image may
	 * have parts that overlap its neighbors.
	 *
	 * @return the standard width and height of this tile image.
	 */
	public abstract Dimension getSize();

	/**
	 * @return a concrete TileImage (no switches) to be drawn for the
	 * specified context.
	 */
	public abstract TileImage realize(DrawContext dc);

	protected abstract Iterator<RealImage> realizeAll_iterator();

	public final Iterable<RealImage> realizeAll() {
		return new Iterable<RealImage>() {
			public Iterator<RealImage> iterator() {
				return realizeAll_iterator();
			}
		};
	}

	public boolean isAnimated()
	{
		return false;
	}

	/**
	 * Brings any internal SwitchImage or Animation objects to
	 * the top of the hierarchy.
	 */
	public TileImage normalForm()
	{
		// subclasses should override this behavior
		return this;
	}

	public static class TileImageLayer extends TileImage
	{
		public final TileImage below;
		public final TileImage above;

		public TileImageLayer(TileImage below, TileImage above)
		{
			assert below != null;
			assert above != null;
			assert below.getSize().equals(above.getSize()) : "cannot layer images of differing standard sizes";

			this.below = below;
			this.above = above;
		}

		@Override
		public TileImageLayer realize(DrawContext dc)
		{
			TileImage below_r = below.realize(dc);
			TileImage above_r = above.realize(dc);
			if (below_r == below && above_r == above) {
				return this;
			}
			return new TileImageLayer(below_r, above_r);
		}

		@Override
		protected Iterator<RealImage> realizeAll_iterator()
		{
			final Iterator<RealImage> major_it = below.realizeAll().iterator();
			class MyIt implements Iterator<RealImage> {

				RealImage major_c;
				Iterator<RealImage> above_it;

				MyIt() {
					nextMajor();
				}
				private void nextMajor()
				{
					if (major_it.hasNext()) {
						major_c = major_it.next();
						above_it = above.realizeAll().iterator();
					}
					else {
						major_c = null;
						above_it = null;
					}
				}

				public boolean hasNext() {
					return above_it != null && above_it.hasNext();
				}
				public RealImage next()
				{
					RealImage above_c = above_it.next();
					RealImage rv = new RealImage(
						TileCondition.and(major_c.condition, above_c.condition),
						new TileImageLayer(major_c.image, above_c.image)
						);
					if (!above_it.hasNext()) {
						nextMajor();
					}
					return rv;
				}
				public void remove() {
					throw new UnsupportedOperationException();
				}
			}
			return new MyIt();
		}

		@Override
		public TileImage normalForm()
		{
			TileImage below1 = below.normalForm();
			TileImage above1 = above.normalForm();

			if (above1 instanceof MultiPart) {

				MultiPart rv = ((MultiPart)above1).makeEmptyCopy();
				for (Part p : ((MultiPart)above1).parts()) {
					TileImageLayer m = new TileImageLayer(
						below1,
						p.getImage()
						);
					rv.addPartLike(m, p);
				}
				return rv.asTileImage();
			}
			else if (below1 instanceof MultiPart) {

				MultiPart rv = ((MultiPart)below1).makeEmptyCopy();
				for (Part p : ((MultiPart)below1).parts()) {
					TileImageLayer m = new TileImageLayer(
						p.getImage(),
						above1
						);
					rv.addPartLike(m, p);
				}
				return rv.asTileImage();
			}
			else {

				return new TileImageLayer(below1, above1);
			}
		}

		@Override
		public void drawFragment(Graphics2D gr, int srcX, int srcY, int srcWidth, int srcHeight)
		{
			below.drawFragment(gr, srcX, srcY, srcWidth, srcHeight);
			above.drawFragment(gr, srcX, srcY, srcWidth, srcHeight);
		}

		@Override
		public Dimension getBounds()
		{
			Dimension belowBounds = below.getBounds();
			Dimension aboveBounds = above.getBounds();
			return new Dimension(
				Math.max(belowBounds.width, aboveBounds.width),
				Math.max(belowBounds.height, aboveBounds.height)
				);
		}

		@Override
		public Dimension getSize()
		{
			return below.getSize();
		}

		@Override
		public String toString()
		{
			return "(layered-image "+below.toString()+" "+above.toString()+")";
		}
	}

	public static class TileImageSprite extends TileImage
	{
		public final TileImage source;
		public final int targetSize;
		public int offsetX;
		public int offsetY;
		public int overlapNorth;
		public int overlapEast;

		public TileImageSprite(TileImage source, int targetSize)
		{
			this.source = source;
			this.targetSize = targetSize;
		}

		@Override
		public TileImageSprite realize(DrawContext dc)
		{
			TileImage source_r = source.realize(dc);
			if (source_r == source) {
				return this;
			}
			TileImageSprite me_r = new TileImageSprite(source_r, targetSize);
			me_r.offsetX = this.offsetX;
			me_r.offsetY = this.offsetY;
			me_r.overlapNorth = this.overlapNorth;
			me_r.overlapEast = this.overlapEast;
			return me_r;
		}

		@Override
		protected Iterator<RealImage> realizeAll_iterator()
		{
			final Iterator<RealImage> it = source.realizeAll().iterator();
			return new Iterator<RealImage>()
			{
				public boolean hasNext() {
					return it.hasNext();
				}
				public RealImage next() {
					RealImage c = it.next();
					TileImageSprite me_r = new TileImageSprite(c.image, targetSize);
					me_r.offsetX = offsetX;
					me_r.offsetY = offsetY;
					me_r.overlapNorth = overlapNorth;
					me_r.overlapEast = overlapEast;
					return new RealImage(c.condition, me_r);
				}
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		@Override
		public TileImage normalForm()
		{
			TileImage source_n = source.normalForm();
			if (source_n instanceof MultiPart) {

				MultiPart rv = ((MultiPart)source_n).makeEmptyCopy();
				for (Part p : ((MultiPart)source_n).parts()) {
					TileImageSprite m = sameTransformFor(p.getImage());
					rv.addPartLike(m, p);
				}
				return rv.asTileImage();
			}
			else {
				return sameTransformFor(source_n);
			}
		}

		private TileImageSprite sameTransformFor(TileImage img)
		{
			TileImageSprite m = new TileImageSprite(img, this.targetSize);
			m.offsetX = this.offsetX;
			m.offsetY = this.offsetY;
			return m;
		}

		@Override
		public void drawFragment(Graphics2D gr, int srcX, int srcY, int srcWidth, int srcHeight)
		{
			Graphics2D g1 = (Graphics2D) gr.create();
			g1.translate(0, -overlapNorth*targetSize/STD_SIZE);
			source.drawFragment(g1,
				srcX+offsetX, srcY+offsetY-overlapNorth,
				srcWidth + overlapEast, srcHeight + overlapNorth);
		}

		@Override
		public Dimension getBounds() {
			Dimension d = source.getBounds();
			return new Dimension(
				d.width + overlapEast*targetSize/STD_SIZE,
				d.height + overlapNorth*targetSize/STD_SIZE
				);
		}

		@Override
		public Dimension getSize() {
			return new Dimension(targetSize, targetSize);
		}
	}

	public static class SourceImage extends TileImage
	{
		public final BufferedImage image;
		public final int basisSize;

		public SourceImage(BufferedImage image, int basisSize)
		{
			this.image = image;
			this.basisSize = basisSize;
		}

		@Override
		public TileImage realize(DrawContext dc)
		{
			return this;
		}

		@Override
		protected Iterator<RealImage> realizeAll_iterator()
		{
			return Collections.singletonList(
				new RealImage(
					TileCondition.ALWAYS,
					this)).iterator();
		}

		@Override
		public void drawFragment(Graphics2D gr, int srcX, int srcY, int srcWidth, int srcHeight)
		{
			gr.drawImage(
				image.getSubimage(srcX, srcY, srcWidth, srcHeight),
				0,
				0,
				null);
		}

		@Override
		public Dimension getBounds()
		{
			return new Dimension(basisSize, basisSize);
		}

		@Override
		public Dimension getSize()
		{
			return new Dimension(basisSize, basisSize);
		}

		public int getTargetSize()
		{
			return STD_SIZE;
		}
	}

	/**
	 * Supports rescaling of tile images.
	 */
	public static class ScaledSourceImage extends SourceImage
	{
		public final int targetSize;

		@Override
		public String toString()
		{
			return String.format("(scaled-source-image %dx%d %d -> %d)",
				image.getWidth(),
				image.getHeight(),
				basisSize,
				targetSize);
		}

		public ScaledSourceImage(BufferedImage image, int basisSize, int targetSize)
		{
			super(image, basisSize);
			this.targetSize = targetSize;
		}

		@Override
		public int getTargetSize()
		{
			return targetSize;
		}

		@Override
		public void drawFragment(Graphics2D gr, int srcX, int srcY, int srcWidth, int srcHeight)
		{
			int aSrcX = srcX * basisSize / STD_SIZE;
			int aSrcY = srcY * basisSize / STD_SIZE;
			int aSrcWidth = srcWidth * basisSize / STD_SIZE;
			int aSrcHeight = srcHeight * basisSize / STD_SIZE;

			int aDestWidth = srcWidth * targetSize / STD_SIZE;
			int aDestHeight = srcHeight * targetSize / STD_SIZE;

			gr.drawImage(
				image,
				0,
				0,
				aDestWidth,
				aDestHeight,
				aSrcX,
				aSrcY,
				aSrcX + aSrcWidth,
				aSrcY + aSrcHeight,
				null);
		}

		@Override
		public Dimension getBounds()
		{
			return new Dimension(targetSize, targetSize);
		}

		@Override
		public Dimension getSize()
		{
			return new Dimension(targetSize, targetSize);
		}
	}

	public static class SimpleTileImage extends TileImage
	{
		public SourceImage srcImage;
		public int offsetX;
		public int offsetY;
		public int overlapNorth;
		public int overlapEast;

		@Override
		public Dimension getBounds() {
			Dimension b = srcImage.getBounds();
			if (overlapNorth != 0 || overlapEast != 0) {
				int targetSize = srcImage.getTargetSize();
				return new Dimension(
					b.width + overlapEast*targetSize/STD_SIZE,
					b.height + overlapNorth*targetSize/STD_SIZE
					);
			}
			else {
				return b;
			}
		}

		@Override
		public Dimension getSize() {
			return srcImage.getSize();
		}

		public void drawFragment(Graphics2D gr, int srcX, int srcY, int srcWidth, int srcHeight) {
			int targetSize = srcImage.getTargetSize();
			Graphics2D g1 = (Graphics2D) gr.create();
			g1.translate(0, -overlapNorth*targetSize/STD_SIZE);
			srcImage.drawFragment(g1,
				srcX+offsetX, srcY+offsetY-overlapNorth,
				srcWidth + overlapEast, srcHeight + overlapNorth);
		}

		@Override
		public TileImage realize(DrawContext dc)
		{
			return this;
		}

		@Override
		protected Iterator<RealImage> realizeAll_iterator()
		{
			final Iterator<RealImage> it = srcImage.realizeAll().iterator();
			return new Iterator<RealImage>()
			{
				public boolean hasNext() {
					return it.hasNext();
				}
				public RealImage next() {
					RealImage c = it.next();
					SimpleTileImage me_r = new SimpleTileImage();
					me_r.srcImage = srcImage;
					me_r.offsetX = offsetX;
					me_r.offsetY = offsetY;
					me_r.overlapNorth = overlapNorth;
					me_r.overlapEast = overlapEast;
					return new RealImage(c.condition, me_r);
				}
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		@Override
		public String toString()
		{
			return String.format("(simple %s offset %d,%d overlap %d,%d)",
				srcImage.toString(),
				offsetX, offsetY,
				overlapEast, overlapNorth);
		}
	}

	public interface LoaderContext
	{
		SourceImage getDefaultImage()
			throws IOException;
		SourceImage getImage(String name)
			throws IOException;

		TileImage parseFrameSpec(String tmp)
			throws IOException;
	}

	static SimpleTileImage readSimpleImage(XMLStreamReader in, LoaderContext ctx)
		throws XMLStreamException
	{
		SimpleTileImage img = new SimpleTileImage();
		try {
			String srcImageName = in.getAttributeValue(null, "src");
			if (srcImageName != null) {
				img.srcImage = ctx.getImage(srcImageName);
			}
			else {
				img.srcImage = ctx.getDefaultImage();
			}
		}
		catch (IOException e) {
			throw new XMLStreamException("image source not found", e);
		}

		String tmp = in.getAttributeValue(null, "at");
		if (tmp != null) {
			String [] coords = tmp.split(",");
			if (coords.length == 2) {
				img.offsetX = Integer.parseInt(coords[0]);
				img.offsetY = Integer.parseInt(coords[1]);
			}
			else {
				throw new XMLStreamException("Invalid 'at' syntax");
			}
		}

		String tmp1 = in.getAttributeValue(null, "overlap-north");
		img.overlapNorth = tmp1 != null ? Integer.parseInt(tmp1) : 0;

		String tmp2 = in.getAttributeValue(null, "overlap-east");
		img.overlapEast = tmp2 != null ? Integer.parseInt(tmp2) : 0;

		skipToEndElement(in);
		return img;
	}

	static TileImage readSwitchImage(XMLStreamReader in, LoaderContext ctx)
		throws XMLStreamException
	{
		SwitchImage img = new SwitchImage();
		TileImage defaultImg = null;
		while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
			String tagName = in.getLocalName();
			if (tagName.equals("case")) {
				img.cases.add(readSwitchImageCase(in, ctx));
			}
			else if (tagName.equals("default")) {
				defaultImg = readTileImageM(in, ctx);
			}
			else {
				skipToEndElement(in);
			}
		}
		if (defaultImg != null) {
			img.cases.add(new SwitchImage.Case(TileCondition.ALWAYS, defaultImg));
		}
		else if (in.getLocalName().equals("animation") ||
			in.getLocalName().equals("micropolis-animation"))
		{
			return Animation.read(in, ctx);
		}
		else {
			throw new XMLStreamException("default case is required in switch image");
		}
		return img;
	}

	static SwitchImage.Case readSwitchImageCase(XMLStreamReader in, LoaderContext ctx)
		throws XMLStreamException
	{
		TileCondition c = new TileCondition();

		String s;
		s = in.getAttributeValue(null, "tile-west");
		if (s != null) {
			c.key = "tile-west";
			c.value = s;
		}

		return new SwitchImage.Case(c, readTileImageM(in, ctx));
	}

	static TileImage readLayeredImage(XMLStreamReader in, LoaderContext ctx)
		throws XMLStreamException
	{
		TileImage result = null;

		while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
			assert in.isStartElement();

			TileImage newImg = readTileImage(in, ctx);
			if (result == null) {
				result = newImg;
			}
			else {
				result = new TileImageLayer(
					result,            //below
					newImg             //above
				);
			}
		}

		if (result == null) {
			throw new XMLStreamException("layer must have at least one image");
		}

		return result;
	}

	public static TileImage readTileImage(XMLStreamReader in, LoaderContext ctx)
		throws XMLStreamException
	{
		assert in.isStartElement();
		String tagName = in.getLocalName();

		if (tagName.equals("image")) {
			return readSimpleImage(in, ctx);
		}
		else if (tagName.equals("animation")) {
			return Animation.read(in, ctx);
		}
		else if (tagName.equals("switch")) {
			return readSwitchImage(in, ctx);
		}
		else if (tagName.equals("layered-image")) {
			return readLayeredImage(in, ctx);
		}
		else {
			throw new XMLStreamException("unrecognized tag: "+tagName);
		}
	}

	/**
	 * @param in an XML stream reader with the parent tag of the tag to be read
	 *  still selected
	 */
	public static TileImage readTileImageM(XMLStreamReader in, LoaderContext ctx)
		throws XMLStreamException
	{
		TileImage img = null;

		while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
			assert in.isStartElement();
			String tagName = in.getLocalName();
			if (tagName.equals("image") ||
				tagName.equals("animation") ||
				tagName.equals("switch") ||
				tagName.equals("layered-image"))
			{
				img = readTileImage(in, ctx);
			}
			else {
				skipToEndElement(in);
			}
		}

		if (img == null) {
			throw new XMLStreamException(
				"missing image descriptor"
				);
		}

		return img;
	}

}
