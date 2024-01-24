import processing.core.PApplet;

class Particle {
    float x, y;
    float size;
    int lifespan;

    Particle(float x, float y, float size, int lifespan) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.lifespan = lifespan;
    }

    void update(PApplet sketch) {
        // Make particles float upwards
        y -= (float) (0.5 + Math.random() * (1.5 - 0.5)); // Adjust the floating speed

        // Decrease the lifespan
        lifespan--;

        // Add horizontal movement to the particles
        x += (float) (-0.5 + Math.random() * (0.5 - (-0.5))); // Add horizontal movement to the particles
    }

    void display(PApplet sketch) {
        // Create a color gradient for the particles
        int r = (int) (56 + Math.random() * (255 - 56)); // Adjust the range for red component
        int g = (int) (100 + Math.random() * (200 - 100));
        int b = (int) (100 + Math.random() * (200 - 100));

        // Add color variation to each particle
        sketch.fill(r, g, b, lifespan * 255 / 100 as int); // Random color with varying transparency
        sketch.noStroke();

        // Draw ellipses to create a balloon-like effect with particles floating upwards
        sketch.ellipse(x, y, size, size);

        // Pass the sketch to the update method
        update(sketch);
    }

    boolean isDead() {
        // Check if the particle's lifespan is over
        return lifespan <= 0;
    }
}

