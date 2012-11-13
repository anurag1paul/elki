package experimentalcode.erich.jogl;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.awt.Color;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.media.opengl.DebugGL2;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;

import com.jogamp.opengl.util.awt.TextRenderer;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.persistent.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHandler;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.ScalesResult;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.projections.ProjectionParallel;
import de.lmu.ifi.dbs.elki.visualization.projections.SimpleParallel;
import de.lmu.ifi.dbs.elki.visualization.style.ClassStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.ClusterStylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.style.PropertiesBasedStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleResult;
import de.lmu.ifi.dbs.elki.visualization.style.StylingPolicy;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;
import experimentalcode.erich.jogl.Simple1DOFCamera.CameraListener;
import experimentalcode.shared.parallelcoord.layout.Layout;
import experimentalcode.shared.parallelcoord.layout.Layouter3DPC;
import experimentalcode.shared.parallelcoord.layout.SimpleCircularMSTLayout;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;

/**
 * Simple JOGL2 based parallel coordinates visualization.
 * 
 * @author Erich Schubert
 * 
 *         TODO: Improve generics of Layout3DPC.
 */
public class OpenGL3DParallelCoordinates implements ResultHandler {
  /**
   * Logging class.
   */
  private static final Logging LOG = Logging.getLogger(ResultHandler.class);

  Settings settings = new Settings();

  /**
   * Constructor.
   * 
   * @param layout Layout
   */
  public OpenGL3DParallelCoordinates(Layouter3DPC<? super NumberVector<?>> layout) {
    settings.layout = layout;
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    StyleResult style = getStyleResult(baseResult);
    Iterator<Relation<? extends NumberVector<?>>> iter = VisualizerUtil.iterateVectorFieldRepresentations(newResult);
    while (iter.hasNext()) {
      Relation<? extends NumberVector<?>> rel = iter.next();
      ScalesResult scales = ResultUtil.getScalesResult(rel);
      ProjectionParallel proj = new SimpleParallel(scales.getScales());
      new Instance(rel, proj, settings, style).run();
    }
  }

  /**
   * Hack: Get/Create the style result.
   * 
   * @return Style result
   */
  public StyleResult getStyleResult(HierarchicalResult result) {
    ArrayList<StyleResult> styles = ResultUtil.filterResults(result, StyleResult.class);
    if (styles.size() > 0) {
      return styles.get(0);
    }
    StyleResult styleresult = new StyleResult();
    styleresult.setStyleLibrary(new PropertiesBasedStyleLibrary());
    ResultUtil.ensureClusteringResult(ResultUtil.findDatabase(result), result);
    List<Clustering<? extends Model>> clusterings = ResultUtil.getClusteringResults(result);
    if (clusterings.size() > 0) {
      styleresult.setStylingPolicy(new ClusterStylingPolicy(clusterings.get(0), styleresult.getStyleLibrary()));
      result.getHierarchy().add(result, styleresult);
      return styleresult;
    } else {
      throw new AbortException("No clustering result generated?!?");
    }
  }

  /**
   * Class keeping the visualizer settings.
   * 
   * @author Erich Schubert
   */
  public static class Settings {
    /**
     * Layouting method.
     */
    public Layouter3DPC<? super NumberVector<?>> layout;

    /**
     * Alpha effect: 1=solid, 0=full alpha effect
     * 
     * Note: since OpenGL usually works with 8 bit color depth, this puts a
     * strong limitation on alpha: computation is not in floats, but has 1/256
     * steps for blending. This in particular affects sloped lines!
     */
    double alpha = .5;

    /**
     * Line width
     */
    public float linewidth = 2f;
  }

  /**
   * Visualizer instance.
   * 
   * @author Erich Schubert
   */
  public static class Instance implements GLEventListener {
    /**
     * Flag to enable debug rendering.
     */
    private static final boolean DEBUG = false;

    /**
     * Relation to viualize
     */
    Relation<? extends NumberVector<?>> rel;

    /**
     * Projection
     */
    ProjectionParallel proj;

    /**
     * Frame
     */
    JFrame frame = null;

    /**
     * GLU utility class.
     */
    GLU glu;

    /**
     * Settings
     */
    Settings settings;

    /**
     * Style result
     */
    StyleResult style;

    /**
     * Axis labels
     */
    String[] labels;

    /**
     * Layout
     */
    private Layout layout;

    private Parallel3DRenderer prenderer;

    /**
     * Text renderer
     */
    TextRenderer textrenderer;

    /**
     * The OpenGL canvas
     */
    GLCanvas canvas;

    /**
     * Camera handling class
     */
    Simple1DOFCamera camera;

    /**
     * Arcball controller.
     */
    Arcball1DOFAdapter arcball;

    /**
     * Constructor.
     * 
     * @param rel Relation
     * @param proj Projection
     * @param settings Settings
     * @param style Style result
     */
    public Instance(Relation<? extends NumberVector<?>> rel, ProjectionParallel proj, Settings settings, StyleResult style) {
      super();
      this.rel = rel;
      this.proj = proj;
      this.settings = settings;
      this.style = style;
      this.prenderer = new Parallel3DRenderer();

      GLProfile glp = GLProfile.getDefault();
      GLCapabilities caps = new GLCapabilities(glp);
      caps.setDoubleBuffered(true);
      canvas = new GLCanvas(caps);
      canvas.addGLEventListener(this);

      frame = new JFrame("EKLI OpenGL Visualization");
      frame.setSize(600, 600);
      frame.add(canvas);
    }

    public void run() {
      layout = settings.layout.layout(rel);

      assert (frame != null);
      frame.setVisible(true);
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
          stop();
        }
      });
    }

    public void stop() {
      frame = null;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
      GL2 gl = drawable.getGL().getGL2();
      if (DEBUG) {
        gl = new DebugGL2(gl);
        drawable.setGL(gl);
      }
      // As we aren't really rendering models, but just drawing,
      // We do not need to set up a lot.
      gl.glClearColor(1f, 1f, 1f, 1f);
      gl.glDisable(GL.GL_DEPTH_TEST);
      gl.glDisable(GL.GL_CULL_FACE);

      glu = new GLU();
      camera = new Simple1DOFCamera(glu);
      camera.addCameraListener(new CameraListener() {
        @Override
        public void cameraChanged() {
          canvas.display();
        }
      });

      // Setup arcball:
      arcball = new Arcball1DOFAdapter(camera);
      canvas.addMouseListener(arcball);
      canvas.addMouseMotionListener(arcball);
      canvas.addMouseWheelListener(arcball);

      prenderer.setupVertexBuffer(gl);
      textrenderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 36));
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
      camera.setRatio(width / (double) height);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
      GL2 gl = drawable.getGL().getGL2();
      gl.glClear(GL.GL_COLOR_BUFFER_BIT /* | GL.GL_DEPTH_BUFFER_BIT */);

      camera.apply(gl);

      final int dim = RelationUtil.dimensionality(rel);
      prenderer.drawParallelPlot(drawable, dim, gl);

      if (DEBUG) {
        arcball.debugRender(gl);
      }
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
      GL gl = drawable.getGL();
      prenderer.dispose(gl);
    }

    /**
     * Renderer for 3D parallel plots.
     * 
     * The tricky part here is the vertex buffer layout. We are drawing lines,
     * so we need two vertices for each macro edge (edge between axes in the
     * plot). We furthermore need the following properties: we need to draw
     * edges sorted by depth to allow alpha and smoothing to work, and we need
     * to be able to have different colors for clusters. An efficient batch
     * therefore will consist of one edge-color combination. The input data
     * comes in color-object ordering, so we need to seek through the edges when
     * writing the buffer.
     * 
     * In total, we have 2 * obj.size * edges.size vertices.
     * 
     * Where obj.size = sum(col.sizes)
     * 
     * @author Erich Schubert
     */
    class Parallel3DRenderer {

      /**
       * Vertex buffer IDs
       */
      int[] vbi = null;

      /**
       * Number of quads in buffer
       */
      int lines = -1;

      /**
       * Sizes of the individual clusters
       */
      int[] sizes;

      /**
       * Colors
       */
      float[] colors;

      private int size;

      private void setupVertexBuffer(GL2 gl) {
        final int dim = RelationUtil.dimensionality(rel);
        size = rel.size();
        final float sz = (float) (1 / StyleLibrary.SCALE);

        int lines = layout.edges.size() * size;

        // Setup buffer IDs:
        int[] vbi = new int[1];
        gl.glGenBuffers(1, vbi, 0);
        // Vertexes
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbi[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, lines * 2 * 3 * ByteArrayUtil.SIZE_FLOAT, null, GL2.GL_DYNAMIC_DRAW);
        ByteBuffer vbytebuffer = gl.glMapBuffer(GL.GL_ARRAY_BUFFER, GL2.GL_WRITE_ONLY);
        FloatBuffer vertices = vbytebuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();

        StylingPolicy sp = style.getStylingPolicy();
        ColorLibrary cols = style.getStyleLibrary().getColorSet(StyleLibrary.PLOT);
        if (sp instanceof ClassStylingPolicy) {
          ClassStylingPolicy csp = (ClassStylingPolicy) sp;
          final int maxStyle = csp.getMaxStyle();
          TIntArrayList sizes = new TIntArrayList();
          TFloatArrayList colors = new TFloatArrayList();

          int csum = 0; //
          for (int s = csp.getMinStyle(); s < maxStyle; s++) {
            // Count the number of instances in the style
            int c = 0;
            for (DBIDIter it = csp.iterateClass(s); it.valid(); it.advance(), c++, csum++) {
              int coff = (csum << 2) + (csum << 1); // * 6
              double[] vec = proj.fastProjectDataToRenderSpace(rel.get(it));
              for (Layout.Edge e : layout.edges) {
                // Seek to appropriate position.
                // See buffer layout discussed above.
                vertices.position(coff);
                final int d0 = e.dim1, d1 = e.dim2;
                vertices.put((float) layout.getNode(d0).getX());
                vertices.put((float) layout.getNode(d0).getY());
                vertices.put(1.f - sz * (float) vec[d0]);
                vertices.put((float) layout.getNode(d1).getX());
                vertices.put((float) layout.getNode(d1).getY());
                vertices.put(1.f - sz * (float) vec[d1]);
                coff += (size << 2) + (size << 1);
              }
            }
            // Skip empty classes
            if (c == 0) {
              continue;
            }
            sizes.add(c);
            Color col = SVGUtil.stringToColor(cols.getColor(s));
            colors.add(col.getRed() / 255.f);
            colors.add(col.getGreen() / 255.f);
            colors.add(col.getBlue() / 255.f);
          }
          this.sizes = sizes.toArray();
          this.colors = colors.toArray();
        } else {
          // TODO: single color.
        }

        vertices.flip();
        gl.glUnmapBuffer(GL.GL_ARRAY_BUFFER);
        this.lines = lines;
        this.vbi = vbi; // Store

        // Labels:
        labels = new String[dim];
        for (int i = 0; i < dim; i++) {
          labels[i] = RelationUtil.getColumnLabel(rel, i);
        }
      }

      public void dispose(GL gl) {
        // Free vertex buffers
        if (vbi != null) {
          gl.glDeleteBuffers(vbi.length, vbi, 0);
        }
      }

      protected void drawParallelPlot(GLAutoDrawable drawable, final int dim, GL2 gl) {
        gl.glPushMatrix();
        // gl.glRotatef((float) MathUtil.rad2deg(rotation), 0.f, 0.f, 1.f);
        // Enable shading
        gl.glShadeModel(GL2.GL_SMOOTH);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL.GL_BLEND);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glLineWidth(settings.linewidth);

        // Bind vertex buffer.
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbi[0]);
        // Use 2D coordinates
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL.GL_FLOAT, 3 * ByteArrayUtil.SIZE_FLOAT, 0);

        // See buffer layout above!

        // Simple Z sorting for edge groups
        DoubleIntPair[] depth = new DoubleIntPair[layout.edges.size()];
        {
          double[] buf = new double[3];
          double[] z = new double[dim];
          for (int d = 0; d < dim; d++) {
            camera.project(layout.getNode(d).getX(), layout.getNode(d).getY(), 0, buf);
            z[d] = buf[2];
          }
          int e = 0;
          for (Layout.Edge edge : layout.edges) {
            depth[e] = new DoubleIntPair(-(z[edge.dim1] + z[edge.dim2]), e);
            e++;
          }
          Arrays.sort(depth);
        }

        for (int e = 0; e < depth.length; e++) {
          final int eoff = depth[e].second * (size << 1);

          // Within each block, we go by colors:
          for (int i = 0, off = 0; i < sizes.length; i++) {
            // Setup color
            final float alpha = (float) (settings.alpha + (1 - settings.alpha) / Math.sqrt(rel.size()));
            gl.glColor4f(colors[i * 3], colors[i * 3 + 1], colors[i * 3 + 2], alpha);

            // Execute
            final int size2 = sizes[i] << 1;
            gl.glDrawArrays(GL2.GL_LINES, eoff + off, size2);
            off += size2;
          }
        }
        // Unbind buffer
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glPopMatrix();

        // Render labels
        {
          gl.glPushMatrix();
          // TODO: use 2d rendering, and {@link SimpleCamera.project}?
          textrenderer.begin3DRendering();
          // UNDO the camera rotation. This will mess up text orientation!
          gl.glRotatef((float) MathUtil.rad2deg(camera.getRotationZ()), 0.f, 0.f, 1.f);
          // Rotate to have the text face the camera direction, which looks +Y
          // While the text will be visible from +Z and +Y is baseline.
          gl.glRotatef(90.f, 1.f, 0.f, 0.f);
          // HalfPI: 180 degree extra rotation, for text orientation.
          double cos = Math.cos(camera.getRotationZ()), sin = Math.sin(camera.getRotationZ());

          textrenderer.setColor(0.0f, 0.0f, 0.0f, 1.0f);
          float axisdist = 1; // (float) ratio / (dim - 1f);
          float defaultscale = .025f / dim;
          float targetwidth = 0.95f * axisdist;
          for (int i = 0; i < dim; i++) {
            Rectangle2D b = textrenderer.getBounds(labels[i]);
            float scale = defaultscale;
            if (b.getWidth() * scale > targetwidth) {
              scale = targetwidth / (float) b.getWidth();
            }
            float w = (float) b.getWidth() * scale;
            // Rotate manually, in x-z plane
            float x = (float) (cos * layout.getNode(i).getX() + sin * layout.getNode(i).getY());
            float y = (float) (-sin * layout.getNode(i).getX() + cos * layout.getNode(i).getY());
            textrenderer.draw3D(labels[i], (x - w * .5f), 1.01f, -y, scale);
          }
          textrenderer.end3DRendering();
          gl.glPopMatrix();
        }
      }
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Option for layouting method
     */
    public static final OptionID LAYOUT_ID = OptionID.getOrCreateOptionID("parallel3d.layout", "Layouting method for 3DPC.");

    /**
     * Similarity measure
     */
    Layouter3DPC<? super NumberVector<?>> layout;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<Layouter3DPC<? super NumberVector<?>>> layoutP = new ObjectParameter<Layouter3DPC<? super NumberVector<?>>>(LAYOUT_ID, Layouter3DPC.class, SimpleCircularMSTLayout.class);
      if (config.grab(layoutP)) {
        layout = layoutP.instantiateClass(config);
      }
    }

    @Override
    protected OpenGL3DParallelCoordinates makeInstance() {
      return new OpenGL3DParallelCoordinates(layout);
    }
  }
}