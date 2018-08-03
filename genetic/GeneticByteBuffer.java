import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class GeneticByteBuffer {

    private static final String IMG_PATH = "source.jpg";
    private static final BufferedImage ORI_IMAGE = loadImg(IMG_PATH);

    private static final int GENE_LENGTH = 56;

    private static final int[] NORMAL_2_GRAY = buildGray(256);
    private static final int[] GRAY_2_NORMAL = buildNormal(NORMAL_2_GRAY);

    private static int[] buildNormal(int[] gray2Normal) {
        int[] result = new int[gray2Normal.length];
        for (int i = 0; i < gray2Normal.length; i++) {
//            result[gray2Normal[i]] = i;
            result[i] = i;
        }
        return result;
    }


    /**
     * <a></>https://en.wikipedia.org/wiki/Gray_code</a>
     */
    private static int[] buildGray(int total) {
        int[] list = new int[total];
//        list[0] = 0;
//
//        int cur = 0;
//        int i = 0;
//        while (cur < total - 1) {
//            int val = (1 << i);
//            int size = cur + 1;
//            for (int j = size - 1; j >= 0 && cur < total - 1; j--) {
//                cur++;
//                list[cur] = val + list[j];
//            }
//            i++;
//        }
        for (int i = 0; i < total; i++) {
            list[i] = i;
        }
        return list;
    }

    private static BufferedImage buildImage(Chromosome chromosome) {
        BufferedImage resizedImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = resizedImage.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, 256, 256);
        // g.fillRect(0, 0, 256, 256);
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));

        int[] genes = chromosome.genes;
        for (int i = 0; i < genes.length; i += GENE_LENGTH / 8) {

            int x = Math.min(GRAY_2_NORMAL[genes[i]], GRAY_2_NORMAL[genes[i + 2]]);
            int y = Math.min(GRAY_2_NORMAL[genes[i + 1]], GRAY_2_NORMAL[genes[i + 3]]);

            Color color = new Color(GRAY_2_NORMAL[genes[i + 4]], GRAY_2_NORMAL[genes[i + 5]], GRAY_2_NORMAL[genes[i + 6]]);
            graphics.setColor(color);
            graphics.fill(new Rectangle2D.Double(x, y,
                    Math.abs(genes[i + 0] - genes[i + 2]),
                    Math.abs(genes[i + 1] - genes[i + 3])));
        }
        return resizedImage;
    }

    private static int ID = 0;

    public static class Chromosome implements Comparable<Chromosome> {
        private int id = ID++;
        private double error = 0;
        // x1 y1 x11 y11 R G B ...
        private int[] genes;

        private BufferedImage image;

        public Chromosome(int[] genes) {
            this.genes = genes;
            this.image = buildImage(this);
            this.error = calcError();
        }

        private double calcError() {
            double error = 0;
            for (int i = 0; i < image.getWidth(); i++) {
                for (int j = 0; j < image.getHeight(); j++) {
                    int origin = ORI_IMAGE.getRGB(i, j);
                    int newOne = image.getRGB(i, j);
                    error += Math.abs(origin - newOne);
                }
            }
            // to make the error more readable
            return error / image.getWidth() / image.getHeight();
        }

        @Override
        public String toString() {
            return "Chromosome{" +
                    "id=" + id +
                    ",error=" + error +
                    ", genes=" + genes +
                    ", image=" + image +
                    '}';
        }

        @Override
        public int compareTo(Chromosome o) {
            if (error == o.error) {
                return 0;
            } else
                return error > o.error ? 1 : -1;
        }
    }

    private static Chromosome[] breeding(Chromosome[] chromosomes, int eliminateNo) {
//        System.out.println("breeding:");
        int one = (int) (Math.random() * (chromosomes.length - eliminateNo));
        int two = (int) (Math.random() * (chromosomes.length - eliminateNo));
        while (one == two) {
            two = (int) (Math.random() * (chromosomes.length - eliminateNo));
        }
        Chromosome p1 = chromosomes[one];
        Chromosome p2 = chromosomes[two];

//        System.out.println("p1:" + p1);
//        System.out.println("p2:" + p2);

        // break the bit instead of int
        int breakingPoint = (int) (Math.random() * p1.genes.length * 8);

//        System.out.println("index:" + breakingPoint);

        int firstInx = breakingPoint / 8;
        int secondInx = breakingPoint % 8;

        int issue_ori_1 = p1.genes[firstInx];
        int issue_ori_2 = p2.genes[firstInx];

        int mask = 255;//11111111
        int maskFront = mask;
        for (int i = 0; i < secondInx; i++) {
            maskFront = maskFront & ~(1 << i);
        }
        int maskEnd = mask;
        for (int i = 8; i >= secondInx; i--) {
            maskEnd = maskEnd & ~(1 << i);
        }

        int tmp1 = (issue_ori_1 & maskFront) ^ (issue_ori_2 & maskEnd);
        int tmp2 = (issue_ori_2 & maskFront) ^ (issue_ori_1 & maskEnd);

        int[] son1 = new int[p1.genes.length];
        int[] son2 = new int[p2.genes.length];

        System.arraycopy(p1.genes, 0, son1, 0, firstInx);
        System.arraycopy(p2.genes, 0, son2, 0, firstInx);
        son1[firstInx] = tmp1;
        son2[firstInx] = tmp2;

        if (firstInx < son1.length - 1) {
            System.arraycopy(p2.genes, firstInx + 1, son1, firstInx + 1, p2.genes.length - 1 - firstInx);
            System.arraycopy(p1.genes, firstInx + 1, son2, firstInx + 1, p2.genes.length - 1 - firstInx);
        }
        System.out.println("---parents---");
        System.out.println(Arrays.stream(p1.genes).mapToObj(o -> String.valueOf(o)).collect(Collectors.joining(",")));
        System.out.println(Arrays.stream(p2.genes).mapToObj(o -> String.valueOf(o)).collect(Collectors.joining(",")));
        System.out.println("---sons---");
        System.out.println(Arrays.stream(son1).mapToObj(o -> String.valueOf(o)).collect(Collectors.joining(",")));
        System.out.println(Arrays.stream(son2).mapToObj(o -> String.valueOf(o)).collect(Collectors.joining(",")));
        return new Chromosome[]{new Chromosome(son1), new Chromosome(son2)};
    }

    private static void mutation(Chromosome[] chromosomes) {
        int chosen = (int) (Math.random() * chromosomes.length);
        int index = (int) (Math.random() * chromosomes[chosen].genes.length * 8);

        int firstInx = index / 8;
        int secondInx = index % 8;

//        System.out.println("mutate:" + chromosomes[chosen]);
//        System.out.println("at:" + index);
        chromosomes[chosen].genes[firstInx] = chromosomes[chosen].genes[firstInx] ^ (1 << secondInx);
    }

    private static BufferedImage loadImg(String imgPath) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File(imgPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return img;
    }

    /**
     * gene format: x1 y1 x2 y2 r g b
     *
     * @param geneSize how many genes in a chromosome
     * @return bitset
     */
    private static int[] generateRandomChromosomes(int geneSize) {
        int[] result = new int[geneSize * GENE_LENGTH / 8];

        for (int i = 0; i < GENE_LENGTH / 8 * geneSize; i += 7) {
            result[i] = NORMAL_2_GRAY[(int) (Math.random() * 256)];
            result[i + 1] = NORMAL_2_GRAY[(int) (Math.random() * 256)];
            result[i + 2] = NORMAL_2_GRAY[(int) (Math.random() * 256)];
            while (result[i + 2] == result[i]) {
                result[i + 2] = NORMAL_2_GRAY[(int) (Math.random() * 256)];
            }
            result[i + 3] = NORMAL_2_GRAY[(int) (Math.random() * 256)];
            while (result[i + 3] == result[i + 1]) {
                result[i + 3] = NORMAL_2_GRAY[(int) (Math.random() * 256)];
            }
            result[i + 4] = NORMAL_2_GRAY[(int) (Math.random() * 256)];
            result[i + 5] = NORMAL_2_GRAY[(int) (Math.random() * 256)];
            result[i + 6] = NORMAL_2_GRAY[(int) (Math.random() * 256)];
            //  System.out.println(Arrays.stream(data).mapToObj(o -> String.valueOf(o)).collect(Collectors.joining(",")));
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        String path = ".test/";
        ImageIO.write(ORI_IMAGE, "png", new File(path + "origin.png"));
        int totalGenerations = 100001;
        int poolSize = 10;
        int geneSize = 30;

        Chromosome[] chromosomes = new Chromosome[poolSize];
        for (int i = 0; i < poolSize; i++) {
            chromosomes[i] = new Chromosome(generateRandomChromosomes(geneSize));
        }
        while (totalGenerations-- != 0) {
            if (totalGenerations % 500 == 0) {
                for (Chromosome chromosome : chromosomes) {
                    System.out.println(Arrays.stream(chromosome.genes).mapToObj(o -> String.valueOf(o)).collect(Collectors.joining(",")));
                }
                for (int i = 0; i < chromosomes.length; i++) {
                    ImageIO.write(chromosomes[i].image, "png",
                            new File(path + totalGenerations + "_" + i + ".png"));
                }
            }
            System.out.println("-------round:   " + totalGenerations);
            Arrays.sort(chromosomes);

            Chromosome[] sons = breeding(chromosomes, 2);
            chromosomes[chromosomes.length - 2] = sons[0];
            chromosomes[chromosomes.length - 1] = sons[1];
            mutation(chromosomes);
        }
    }
}

