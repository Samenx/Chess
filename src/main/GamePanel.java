package main;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;

import javax.swing.JPanel;

import piece.Bishop;
import piece.King;
import piece.Knight;
import piece.Pawn;
import piece.Piece;
import piece.Queen;
import piece.Rook;

public class GamePanel extends JPanel implements Runnable {
	
	public static final int WIDTH = 1100;
	public static final int HEIGHT = 800;
	final int FBS = 60;
	Thread gameThread;
	Board board = new Board();
	Mouse mouse = new Mouse();
	
	
	//pieces 
	public static ArrayList<Piece> pieces = new ArrayList<>(); // backup list in case of rest
	public static ArrayList<Piece> simPieces = new ArrayList<>(); // what we going to use
	 ArrayList<Piece> promoPiece = new ArrayList<>();
	Piece activeP, checkingP ; //  this is the piece that the player pressed 
	public static Piece castlingP;
	
	public static final int WHITE =0;
	public static final int BLACK =1;
	int currentColor = WHITE; // becouse chess game start with white 
	
	
	boolean canMove;
	boolean validSquare;
	boolean promotion;
	boolean gameover;
	boolean stalemate;
	
	public GamePanel()
	{
		
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setBackground(Color.black);
		
		addMouseMotionListener(mouse);
		addMouseListener(mouse);
		
		setPieces();
		copyPieces(pieces, simPieces);
	}
	
	public void launchGame()
	{
		gameThread = new Thread(this);
		gameThread.start();
		
	}
	
	public void setPieces()
	{
		pieces.add(new Pawn (WHITE,0,6));
		pieces.add(new Pawn (WHITE,1,6));
		pieces.add(new Pawn (WHITE,2,6));
		pieces.add(new Pawn (WHITE,3,6));
		pieces.add(new Pawn (WHITE,4,6));
		pieces.add(new Pawn (WHITE,5,6));
		pieces.add(new Pawn (WHITE,6,6));
		pieces.add(new Pawn (WHITE,7,6));
		pieces.add(new Rook (WHITE,0,7));
		pieces.add(new Rook (WHITE,7,7));
		pieces.add(new Knight (WHITE,1,7));
		pieces.add(new Knight (WHITE,6,7));
		pieces.add(new Bishop (WHITE,2,7));
		pieces.add(new Bishop (WHITE,5,7));
		pieces.add(new Queen (WHITE,3,7));
		pieces.add(new King (WHITE,4,7));
		
		
		
		
		pieces.add(new Pawn (BLACK,0,1));
		pieces.add(new Pawn (BLACK,1,1));
		pieces.add(new Pawn (BLACK,2,1));
		pieces.add(new Pawn (BLACK,3,1));
		pieces.add(new Pawn (BLACK,4,1));
		pieces.add(new Pawn (BLACK,5,1));
		pieces.add(new Pawn (BLACK,6,1));
		pieces.add(new Pawn (BLACK,7,1));
		pieces.add(new Rook (BLACK,0,0));
		pieces.add(new Rook (BLACK,7,0));
		pieces.add(new Knight (BLACK,1,0));
		pieces.add(new Knight (BLACK,6,0));
		pieces.add(new Bishop (BLACK,2,0));
		pieces.add(new Bishop (BLACK,5,0));
		pieces.add(new Queen (BLACK,3,0));
		pieces.add(new King (BLACK,4,0));
	}
	
	private void copyPieces(ArrayList<Piece> source, ArrayList<Piece> target )
	{
		target.clear();
		
		for(int i=0;i<source.size();i++)
		{
			target.add(source.get(i));
		}
	}
	
	
	
	
	public void run()
	{
		//game loop: sequace of process that runs as long the game runs
		double drawInterval = 1000000000/FBS;
		double delta =0;
		long lastTime =0;
		long currentTime;
		
		while(gameThread != null)
		{
			
			currentTime = System.nanoTime();
			
			delta += (currentTime - lastTime)/ drawInterval;
			lastTime = currentTime;
			
			if(delta >= 1)
			{
				update();
				repaint();
				delta--;
			}
			
		}
		
		
	}
	
	
	private void update()
	{
		
		
		if(promotion)
		{
			promoting();
		}
		else if(gameover == false && stalemate == false)
		{
			// mouse putton pressed 
			if(mouse.pressed)
			{
				if(activeP == null)
					// if the activeP is null, check if you can pick up the piece 
				{
					for(Piece piece : simPieces)
						// if the mouse is on an ally piece, pick it up as the activeP
					{
						if(piece.color == currentColor && piece.col == mouse.x/Board.SQUARE_SIZE &&
								piece.row == mouse.y/Board.SQUARE_SIZE)
						{
							activeP = piece;
						}
							
					}
				}
				else
				{
					// if the player holding the piece simulate the move
					simulate();
				}
			}
			
			// mouse Button Released 
			
			if(mouse.pressed == false)
			{
				if(activeP != null)
				{
					if(validSquare)
					{
						// move confirmed
						
						//update the piece list in case a piece has been captured and removed during the simulation 
						copyPieces(simPieces, pieces);
						activeP.updatePosition();
						
						if(castlingP != null)
						{
							castlingP.updatePosition();
						}
						
						if(isKingInCheck() && isCheckmate())
						{
							gameover = true;
						}
						
						else if(isstalemate())
						{
							stalemate = true;
						}
						
						else {
							if(canpromote())
							{
								promotion = true;
							}
							else
							{
								changePlayer();
							}
						}
						
						
						
						
					}
					else
					{
						//the move is not valid so cancel everything 
						copyPieces(pieces, simPieces);
						activeP.resetPosition();
						activeP = null;
					}
					
					
				}
			}
		}
		
		
		
	}
	
	private void simulate()
	{
		canMove = false;
		validSquare = false;
		
		//reset the piece list in every loop
		// this is for restoring the removed piece during the simulation 
		copyPieces(pieces, simPieces);
		
		if(castlingP != null)
		{
			castlingP.col = castlingP.preCol;
			castlingP.x = castlingP.getX(castlingP.col);
			castlingP = null;
		}
		
		//if the piece is being held update the position 
		activeP.x = mouse.x - Board.HALF_SQUARE_SIZE;
		activeP.y = mouse.y  - Board.HALF_SQUARE_SIZE;
		activeP.col = activeP.getCol(activeP.x);
		activeP.row = activeP.getRow(activeP.y);
				
		// check if the piece id hovering over a reachable square
		if(activeP.canMove(activeP.col, activeP.row))
		{
			canMove = true;
			
			
			
			//if hitting a piece, remove it from the list
			if(activeP.hittingP != null)
			{
				simPieces.remove(activeP.hittingP.getIndex());
			}
			checkCastling();
			if(isIleagle(activeP) == false && opponentCanCaptureKing() == false)
			{
				validSquare = true;
			}
			
			validSquare = true;
		}
		
	}
	
	private boolean isIleagle(Piece King)
	{
		if(King.type == Type.KING)
		{
			for(Piece piece : simPieces)
			{
				if(piece != King && piece.color != King.color && piece.canMove(King.col,  King.row))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean opponentCanCaptureKing() {
		Piece king = getKing(false);
		
		for(Piece piece: simPieces)
		{
			if(piece.color != king.color && piece.canMove(king.col, king.row))
			{
				return true;
			}
		}
		return false;
	}
	
	
	private boolean isKingInCheck()
	{
		
		Piece king = getKing(true);
		
		if(activeP.canMove(king.col, king.row))
		{
			checkingP = activeP;
			return true;
		}
		
		else
		{
			checkingP = null;
		}
		return false;
		
	}
	
	private Piece getKing(boolean opponent)
	{
		Piece king = null;
		
		for(Piece piece: simPieces)
		{
			if(opponent)
			{
				if(piece.type == Type.KING && piece.color != currentColor)
				{
					king = piece;
				}
			}
			else
			{
				if(piece.type == Type.KING && piece.color == currentColor)
				{
					king = piece;
				}
			}
		}
		return king;
		
	}
	
	private boolean isCheckmate()
	{
		
		Piece king = getKing(true);
		
		if(kingCanMove(king))
		{
			return false;
		}
		else
		{
			int colDiff = Math.abs(checkingP.col - king.col);
			int rowDiff = Math.abs(checkingP.row - king.row);
			
			
			if(colDiff == 0)
			{
				if(checkingP.row < king.row)
				{
					for(int row = checkingP.row; row < king.row;row++)
					{
						for(Piece piece : simPieces)
						{
							if(piece != king && piece.color != currentColor && piece.canMove(checkingP.col, row))
							{
								return false;
							}
						}
					}
				}
				
				if(checkingP.row> king.row)
				{
					for(int row = checkingP.row; row > king.row;row--)
					{
						for(Piece piece : simPieces)
						{
							if(piece != king && piece.color != currentColor && piece.canMove(checkingP.col, row))
							{
								return false;
							}
						}
					}
				}
			}
			else if(rowDiff == 0)
			{
				if(checkingP.col < king.col)
				{
					for(int col = checkingP.col; col < king.row;col++)
					{
						for(Piece piece: simPieces)
						{
							if(piece != king && piece.color != currentColor && piece.canMove(col, checkingP.row))
							{
								return false;
							}
						}
					}
				}
				
				if(checkingP.col > king.col)
				{
					for(int col = checkingP.col; col > king.row;col--)
					{
						for(Piece piece: simPieces)
						{
							if(piece != king && piece.color != currentColor && piece.canMove(col, checkingP.row))
							{
								return false;
							}
						}
					}
				}
			}
			else if(colDiff == rowDiff)
			{
				if(checkingP.row < king.row)
				{
					if(checkingP.col < king.col)
					{
						for(int col = checkingP.col, row = checkingP.row; col < king.col; col++, row++)
						{
							for(Piece piece: simPieces)
							{
								if(piece != king && piece.color != currentColor && piece.canMove(col, row))
								{
									return false;
								}
							}
						}
					}
					if(checkingP.col > king.col)
					{
						for(int col = checkingP.col, row = checkingP.row; col > king.col; col--, row++)
						{
							for(Piece piece: simPieces)
							{
								if(piece != king && piece.color != currentColor && piece.canMove(col, row))
								{
									return false;
								}
							}
						}
					}
				}
				
				
				if(checkingP.row > king.row)
				{
					if(checkingP.col < king.col)
					{
						for(int col = checkingP.col, row = checkingP.row; col < king.col; col++, row--)
						{
							for(Piece piece: simPieces)
							{
								if(piece != king && piece.color != currentColor && piece.canMove(col, row))
								{
									return false;
								}
							}
						}
					}
					if(checkingP.col > king.col)
					{
						for(int col = checkingP.col, row = checkingP.row; col > king.col; col--, row--)
						{
							for(Piece piece: simPieces)
							{
								if(piece != king && piece.color != currentColor && piece.canMove(col, row))
								{
									return false;
								}
							}
						}
					}
				}
			}
			else //knight
			{
				
			}
			
		}
		return true;
	}
	private boolean kingCanMove(Piece king)
	{
		if(isValidMove(king, -1,-1)) {return true;}
		if(isValidMove(king, 0,-1)) {return true;}
		if(isValidMove(king, 1,-1)) {return true;}
		if(isValidMove(king, -1,0)) {return true;}
		if(isValidMove(king, 1,0)) {return true;}
		if(isValidMove(king, -1,1)) {return true;}
		if(isValidMove(king, 0,1)) {return true;}
		if(isValidMove(king, 1,1)) {return true;}
		
		return false;
	}
	private boolean isValidMove(Piece king, int colPlus, int rowPlus)
	{
		boolean isValidMove = false;
		
		king.col += colPlus;
		king.row += rowPlus;
		
		if(king.canMove(king.col,  king.row))
		{
			if(king.hittingP != null)
			{
				simPieces.remove(king.hittingP.getIndex());
			}
			if(isIleagle(king) == false)
			{
				isValidMove = true;
			}
		}
		
		king.resetPosition();
		copyPieces(pieces, simPieces);
		
		return isValidMove;
		
		
		
	}
	
	private boolean isstalemate()
	{
		int count =0;
		
		for(Piece piece: simPieces)
		{
			if(piece.color != currentColor)
			{
				count++;
			}
		}
		
		if(count == 1)
		{
			if(kingCanMove(getKing(true)) == false)
			{
				return true;
			}
		}
		
		return false;
	}
	
	private void checkCastling()
	{
		
		if(castlingP != null)
		{
			if(castlingP.col == 0)
			{
				castlingP.col += 3;
			}
			
			else if(castlingP.col == 7)
			{
				castlingP.col -=2;
			}
			
			castlingP.x = castlingP.getX(castlingP.col);
		}
		
	}
	
	private void changePlayer()
	{
		if(currentColor == WHITE)
		{
			currentColor = BLACK;
			
			for(Piece piece : pieces)
			{
				if(piece.color == BLACK)
				{
					piece.twoStepped = false;
				}
			}
		}
		else
		{
			currentColor = WHITE;
			
			for(Piece piece : pieces)
			{
				if(piece.color == WHITE)
				{
					piece.twoStepped = false;
				}
			}
		}
		
		activeP = null;
	}
	
	public boolean canpromote()
	{
		
		if(activeP.type == Type.PAWN)
		{
			if(currentColor == WHITE && activeP.row == 0 || currentColor == BLACK && activeP.row == 7  )
			{
				promoPiece.clear();
				promoPiece.add(new Rook(currentColor,9,2));
				promoPiece.add(new Knight(currentColor,9,3));
				promoPiece.add(new Bishop(currentColor,9,4));
				promoPiece.add(new Queen(currentColor,9,5));
				return true;
				
			}
		}
		
		
		return false;
	}
	
	public void promoting()
	{
		if(mouse.pressed)
		{
		for(Piece piece: promoPiece)
		{
			if(piece.col == mouse.x/Board.SQUARE_SIZE && piece.row == mouse.y/Board.SQUARE_SIZE)
			{
				switch(piece.type)
				{
				case ROOK: simPieces.add(new Rook(currentColor, activeP.col, activeP.row)); break;
				case KNIGHT: simPieces.add(new Knight(currentColor, activeP.col, activeP.row)); break;
				case BISHOP: simPieces.add(new Bishop(currentColor, activeP.col, activeP.row)); break;
				case QUEEN: simPieces.add(new Queen(currentColor, activeP.col, activeP.row)); break;
				default: break;
				
				}
				simPieces.remove(activeP.getIndex());
				copyPieces(simPieces, pieces);
				activeP = null;
				promotion= false;
				changePlayer();
				
				
			}
		}
		
		}
	}
	
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		
		Graphics2D g2 = (Graphics2D)g;
		board.draw(g2);
		
		for(Piece p : simPieces)
		{
			p.draw(g2);
		}
		
		if(activeP != null)
		{
			
			if(canMove)
			{
				if(isIleagle(activeP) || opponentCanCaptureKing()  )
				{
					g2.setColor(Color.red);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
					g2.fillRect(activeP.col*Board.SQUARE_SIZE,activeP.row*Board.SQUARE_SIZE,
							Board.SQUARE_SIZE, Board.SQUARE_SIZE);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
				}
				else
				{
					g2.setColor(Color.white);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
					g2.fillRect(activeP.col*Board.SQUARE_SIZE,activeP.row*Board.SQUARE_SIZE,
							Board.SQUARE_SIZE, Board.SQUARE_SIZE);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
				}
				
				
			}
			
			
			
			activeP.draw(g2);
			
		}
		
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setFont(new Font("Book Antiqua", Font.PLAIN, 40));
		g2.setColor(Color.white);
		
		if(promotion)
		{
			g2.drawString("Promote To: ", 840, 150);
			for(Piece piece: promoPiece)
			{
				g2.drawImage(piece.image, piece.getX(piece.col), piece.getY(piece.row), 
						Board.SQUARE_SIZE, Board.SQUARE_SIZE, null);
			}
		}
		
		else
		{
			if(currentColor == WHITE)
			{
				g2.drawString("White's turn", 840, 550);
				if(checkingP != null && checkingP.color == BLACK)
				{
					g2.setColor(Color.red);
					g2.drawString("CHECK !", 840, 700);
				}
			}
			else
			{
				g2.drawString("Black's turn", 840, 550);
				if(checkingP != null && checkingP.color == WHITE)
				{
					g2.setColor(Color.red);
					g2.drawString("CHECK !", 840, 100);
				}
			}
			
		}
		
		if(gameover)
		{
			String s = "";
			
			if(currentColor == WHITE)
			{
				s = "White Wins";
			}
			else
			{
				s = "Black Wins";
				
			}
			
			g2.setFont(new Font("Arial", Font.PLAIN,90));
			g2.setColor(Color.green);
			g2.drawString(s,  200,  420);
		}
		if(stalemate)
		{
			g2.setFont(new Font("Arial", Font.PLAIN,90));
			g2.setColor(Color.lightGray);
			g2.drawString("Stalemate",  200,  420);
		}
		
		
	}
	
	
	
	

}
