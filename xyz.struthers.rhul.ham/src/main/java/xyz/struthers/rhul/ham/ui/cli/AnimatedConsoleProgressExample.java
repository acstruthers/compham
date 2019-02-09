/**
 * 
 */
package xyz.struthers.rhul.ham.ui.cli;

/**
 * @author Adam
 *
 */
public class AnimatedConsoleProgressExample {

	private String lastLine;
	private byte anim;

	/**
	 * https://www.logicbig.com/how-to/code-snippets/jcode-java-command-line-animation
	 * 
	 * \r can be used to reset to the beginning of the current line
	 */
	public AnimatedConsoleProgressExample() {
		super();
		this.lastLine = "";
	}

	public static void main(String[] args) throws InterruptedException {
		AnimatedConsoleProgressExample console = new AnimatedConsoleProgressExample();
		for (int i=0; i<20; i++) {
			console.animate(i + "");
			//simulate a piece of task
			Thread.sleep(400);
		}
	}

	public void print(String line) {
		// clear the last line if longer
		if (lastLine.length() > line.length()) {
			String tmp = "";
			for (int i = 0; i < lastLine.length(); i++) {
				tmp += " ";
			}
			if (tmp.length() > 1) {
				System.out.print("\r" + tmp);
			}
			System.out.print("\r" + line);
			lastLine = line;
		}
	}

	public void animate(String line) {
		switch (anim) {
		case 1:
			print("[ \\ ]" + line);
			break;
		case 2:
			print("[ | ]" + line);
			break;
		case 3:
			print("[ / ]" + line);
			break;
		default:
			anim = 0;
			print("[ - ]" + line);

		}
		anim++;
	}

}
