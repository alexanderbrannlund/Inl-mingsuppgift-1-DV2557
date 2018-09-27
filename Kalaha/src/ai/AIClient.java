package ai;

import ai.Global;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import kalaha.*;

/**
 * This is the main class for your Kalaha AI bot. Currently
 * it only makes a random, valid move each turn.
 * 
 * @author Johan Hagelbäck
 */
public class AIClient implements Runnable
{
    private int player;
    private JTextArea text;
    
    private PrintWriter out;
    private BufferedReader in;
    private Thread thr;
    private Socket socket;
    private boolean running;
    private boolean connected;
    	
    /**
     * Creates a new client.
     */
    public AIClient()
    {
	player = -1;
        connected = false;
        
        //This is some necessary client stuff. You don't need
        //to change anything here.
        initGUI();
	
        try
        {
            addText("Connecting to localhost:" + KalahaMain.port);
            socket = new Socket("localhost", KalahaMain.port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            addText("Done");
            connected = true;
        }
        catch (Exception ex)
        {
            addText("Unable to connect to server");
            return;
        }
    }
    
    /**
     * Starts the client thread.
     */
    public void start()
    {
        //Don't change this
        if (connected)
        {
            thr = new Thread(this);
            thr.start();
        }
    }
    
    /**
     * Creates the GUI.
     */
    private void initGUI()
    {
        //Client GUI stuff. You don't need to change this.
        JFrame frame = new JFrame("My AI Client");
        frame.setLocation(Global.getClientXpos(), 445);
        frame.setSize(new Dimension(420,250));
        frame.getContentPane().setLayout(new FlowLayout());
        
        text = new JTextArea();
        JScrollPane pane = new JScrollPane(text);
        pane.setPreferredSize(new Dimension(400, 210));
        
        frame.getContentPane().add(pane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.setVisible(true);
    }
    
    /**
     * Adds a text string to the GUI textarea.
     * 
     * @param txt The text to add
     */
    public void addText(String txt)
    {
        //Don't change this
        text.append(txt + "\n");
        text.setCaretPosition(text.getDocument().getLength());
    }
    
    /**
     * Thread for server communication. Checks when it is this
     * client's turn to make a move.
     */
    public void run()
    {
        String reply;
        running = true;
        
        try
        {
            while (running)
            {
                //Checks which player you are. No need to change this.
                if (player == -1)
                {
                    out.println(Commands.HELLO);
                    reply = in.readLine();

                    String tokens[] = reply.split(" ");
                    player = Integer.parseInt(tokens[1]);
                    
                    addText("I am player " + player);
                }
                
                //Check if game has ended. No need to change this.
                out.println(Commands.WINNER);
                reply = in.readLine();
                if(reply.equals("1") || reply.equals("2") )
                {
                    int w = Integer.parseInt(reply);
                    if (w == player)
                    {
                        addText("I won!");
                    }
                    else
                    {
                        addText("I lost...");
                    }
                    running = false;
                }
                if(reply.equals("0"))
                {
                    addText("Even game!");
                    running = false;
                }

                //Check if it is my turn. If so, do a move
                out.println(Commands.NEXT_PLAYER);
                reply = in.readLine();
                if (!reply.equals(Errors.GAME_NOT_FULL) && running)
                {
                    int nextPlayer = Integer.parseInt(reply);

                    if(nextPlayer == player)
                    {
                        out.println(Commands.BOARD);
                        String currentBoardStr = in.readLine();
                        boolean validMove = false;
                        while (!validMove)
                        {
                            long startT = System.currentTimeMillis();
                            //This is the call to the function for making a move.
                            //You only need to change the contents in the getMove()
                            //function.
                            GameState currentBoard = new GameState(currentBoardStr);
                            int cMove = getMove(currentBoard);
                            
                            //Timer stuff
                            long tot = System.currentTimeMillis() - startT;
                            double e = (double)tot / (double)1000;
                            
                            out.println(Commands.MOVE + " " + cMove + " " + player);
                            reply = in.readLine();
                            if (!reply.startsWith("ERROR"))
                            {
                                validMove = true;
                                addText("Made move " + cMove + " in " + e + " secs");
                            }
                        }
                    }
                }
                
                //Wait
                Thread.sleep(100);
            }
	}
        catch (Exception ex)
        {
            running = false;
        }
        
        try
        {
            socket.close();
            addText("Disconnected from server");
        }
        catch (Exception ex)
        {
            addText("Error closing connection: " + ex.getMessage());
        }
    }
    
    /**
     * This is the method that makes a move each time it is your turn.
     * Here you need to change the call to the random method to your
     * Minimax search.
     * 
     * @param currentBoard The current board state
     * @return Move to make (1-6)
     */
    public int getMove(GameState currentBoard)  //Satsar på B i betyg
    {
        long startT = System.currentTimeMillis();
        long tot = System.currentTimeMillis() - startT;
        double e = (double)tot / (double)1000;
        int maxDepth=1;
        int[] retMove= new int[3];
        
        while(e<5) // loopar sålänge inte tiden går över 5 sekunder
        {
            GameState clone=currentBoard.clone();                 // skapar klon
            int alpha[]={-999,0,1};                               //Skapar alpha och beta variabler 
            int beta[]={999,0,1};
            int [] myMove =miniMaxAlgorithm(clone,0,true,0,alpha,beta,maxDepth,startT);  // Anropar minimax algoritmen
            
            
            
            if(myMove[2]==1)            //Sparar endast värden där alla löv i angivet djup är besökta 
            {
                retMove[0]=myMove[0];
                retMove[1]=myMove[1];
                retMove[2]=myMove[2];
                maxDepth++;            // ökar maxdjupet för sökningen 
            }
            
            tot = System.currentTimeMillis() - startT; // kollar tiden 
            e = (double)tot / (double)1000;
        
        }
        
        return retMove[1]; // returnerar det bästa draget
    }
    
    /**
     * Returns a random ambo number (1-6) used when making
     * a random move.
     * 
     * @return Random ambo number
     */
    public int getRandom()
    {
        return 1 + (int)(Math.random() * 6);
    }
    
//Satsar på B i betyg
public int[] miniMaxAlgorithm(GameState clone,int deep,boolean maxPlayer, int move,int [] alpha, int [] beta, int maxDepth,long startT )  //Satsar på B i betyg
    {
        
        if(deep==maxDepth||clone.gameEnded())           //Stopcondision 1: om sökningen har nått maxdjupet eller att spelet är över 
        {
            int retVal[]= new int [3];
            retVal[0]=clone.getScore(player);           // returnerar AIs poäng
            retVal[1]= move;                            // returnerar vilket drag som gav poängen
            retVal[2]=1;                                // sökningen stannar på stopcondision 1
            return retVal;
        }
        
        long tot = System.currentTimeMillis() - startT;
        double e = (double)tot / (double)1000;
        
        if(e>5)                                         //Om tiden har överstidigit 5 sekunder
        {
             int retVal[]= new int [3];                 //Samma som stopcondision 1
             retVal[0]=clone.getScore(player);
             retVal[1]=move;
             retVal[2]=0;                               // sökningen stannade på stopcondision 2 
             return retVal;
        }
       
        GameState allStates[]= new GameState[6];    // skapar en arry av gamestates
        for(int i=0; i<6; i++)
        {
            allStates[i]=clone.clone();             //clonar gameStatet 
            
        }
        if(maxPlayer)                       //om det är Maxplayers tur
        {
            for(int i=0; i<6;i++)           // för alla noder 
            {
                if(deep==0)
                {
                    move=i+1;
                }
                
                if(allStates[i].moveIsPossible(i+1))            // om nästa steg är ett möjligt steg 
                {
                    allStates[i].makeMove(i+1);
                    maxPlayer = allStates[i].getNextPlayer()==player;       // kontrollera vems tur det är 
                    
                    int nextMove[]=miniMaxAlgorithm(allStates[i],deep+1,maxPlayer,move,alpha, beta,maxDepth,startT); // rekursivt anrop
                    if(nextMove[0]>alpha[0])        // om scoren är högre än alphas score 
                    {
                        alpha[0]=nextMove[0];       // uppdatera alpha med de nya väderna 
                        alpha[1]=nextMove[1];
                        alpha[2]=nextMove[2];
                    }
                }
                if(alpha[0]>=beta[0])               // alpha/ beta prunning 
                    break;
                
                if(alpha[2]==0)                     // om stopcondision 2, avbryt loppen
                    break;
                    
            }
            return alpha;                          // returnerar det bästa vädet 
            
        }
        
        else                                    // samma steg som ovan fast för minPlayer
        {
            for(int i=0; i<6;i++)
            {
                
                
                if(allStates[i].moveIsPossible(i+1))
                {
                    allStates[i].makeMove(i+1);
                    
                    maxPlayer = allStates[i].getNextPlayer()==player;
                    
                    int nextMove[]=miniMaxAlgorithm(allStates[i],deep+1,maxPlayer,move, alpha, beta,maxDepth,startT);
                    
                    if(nextMove[0]<beta[0])
                    {
                        beta[0]=nextMove[0];
                        beta[1]=nextMove[1];
                        beta[2]=nextMove[2];
                    }
                }
                if(alpha[0]>=beta[0])
                    break;
                
                if(beta[2]==0)
                    break;
                    
            }
            return beta;
        }
    }

}