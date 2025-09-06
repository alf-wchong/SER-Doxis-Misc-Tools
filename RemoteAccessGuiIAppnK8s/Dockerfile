FROM beheadedjavagui

# Install required packages: Xvfb (X virtual framebuffer) and TigerVNC server
RUN apt-get update && apt-get install -y \
    xvfb \
    tigervnc-standalone-server \
    tigervnc-common \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Set environment variable for Xvfb display
ENV DISPLAY=:1

# Expose the default VNC port
EXPOSE 5901

# Create a user for running the GUI (optional but recommended)
RUN useradd -m guiuser

# Switch to the guiuser
USER guiuser
WORKDIR /home/guiuser

# Set VNC password (change "vncpassword" to a secure password)
RUN mkdir -p ~/.vnc && \
    echo "vncpassword" | vncpasswd -f > ~/.vnc/passwd && \
    chmod 600 ~/.vnc/passwd

# Start Xvfb on display :1 and start VNC server on :1 with no security types (or adjust)
CMD Xvfb :1 -screen 0 1024x768x24 & \
    vncserver :1 -geometry 1024x768 -localhost no -SecurityTypes None && \
    java -jar /path/to/your/beheadedjavagui.jar
