package com.kyper.yarn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.kyper.yarn.Analyser.Context;
import com.kyper.yarn.Library.ReturningFunc;
import com.kyper.yarn.VirtualMachine.CommandHandler;
import com.kyper.yarn.VirtualMachine.ExecutionState;
import com.kyper.yarn.VirtualMachine.LineHandler;
import com.kyper.yarn.VirtualMachine.NodeCompleteHandler;
import com.kyper.yarn.VirtualMachine.OptionsHandler;
import com.kyper.yarn.VirtualMachine.TokenType;

public class Dialogue {

	protected VariableStorage continuity;

	public YarnLogger debug_logger;
	public YarnLogger error_logger;

	// node we start from
	public static final String DEFAULT_START = "Start";

	// the program is the compiled yarn program
	protected Program program;

	// the library contains all the functions and operators we know about
	protected Library library;

	private VirtualMachine vm;
	private LineHandler line_handler;
	private OptionsHandler option_handler;
	private CommandHandler command_handler;
	private NodeCompleteHandler node_complte_handler;

	// collection of nodes that we've seen
	public HashMap<String, Integer> visited_node_count = new HashMap<String, Integer>();

	protected boolean execution_complete;

	/**
	 * creates a yarn dialogue
	 *
	 * @param continuity
	 *            - will be used to store/get values
	 * @param debug
	 *            - debug logger implementation
	 * @param error
	 *            - error logger implementation
	 */
	public Dialogue(VariableStorage continuity, YarnLogger debug, YarnLogger error) {
		this.continuity = continuity;
//		loader = new Loader(this);
		library = new Library();

		this.debug_logger = debug;
		this.error_logger = error;
		this.execution_complete = false;

		library.importLibrary(new StandardLibrary());

		// register the "visited" function which returns true if we've visited
		// a node previously (nodes are marked as visited when we leave them)
		library.registerFunction("visited", -1, yarnFunctionIsNodeVisited);

		// register the visitCount function which returns athe number of times
		// a node has been run(increments on node end)
		// no parameters = check the current node
		library.registerFunction("visitCount", -1, yarnFunctionNodeVisitCount);

	}

	public Library getLibrary() {
		return library;
	}

	public boolean isRunning() {
		return vm != null && vm.getExecutionState() != ExecutionState.Stopped;
	}

	public ExecutionState getExecutionState() {
		return vm.getExecutionState();
	}

	/**
	 * creates a dialogue with a default debug and error implementation
	 *
	 * @param continuity
	 *            - will be used to store/get values
	 */
	public Dialogue(VariableStorage continuity) {
		this(continuity, new YarnLogger() {

			@Override
			public void log(String message) {
				System.out.println("YarnGdx:" + message);
			}
		}, new YarnLogger() {
			@Override
			public void log(String message) {
				System.out.println("YarnGdx:" + message);
			}
		});
	}

	/**
	 * load all nodes contained in the text to the dialogue unless otherwise
	 * specified
	 *
	 * @param text
	 *            - the text containing node info
	 * @param file_name
	 *            - the name of the 'file' used for debug purposes
	 * @param show_tokens
	 *            - if true will show the tokens generated by the lexer
	 * @param show_tree
	 *            - if true will show a tree structure generated by the parser
	 * @param only_consider
	 *            - if not null, only the specified node will be considered for
	 *            loading;all else will be ignored.
	 */
	public void loadString(String text, String file_name, boolean show_tokens, boolean show_tree,
			String only_consider) {
			System.err.println("Not implemented - Dialogue.loadString");
			System.exit(1);
//		if (debug_logger == null) {
//			throw new YarnRuntimeException("DebugLogger must be set before loading");
//		}
//
//		if (error_logger == null)
//			throw new YarnRuntimeException("ErrorLogger must be set before loading");
//
//		// try to infer type
//		NodeFormat format;
//		if (text.startsWith("[")) {
//			format = NodeFormat.Json;
//		} else if (text.contains("---")) {
//			format = NodeFormat.Text;
//		} else {
//			format = NodeFormat.SingleNodeText;
//		}
//
//
//		program = loader.load(text, library, file_name, program, show_tokens, show_tree, only_consider, format);
	}

	/**
	 * load all nodes contained in the text to the dialogue unless otherwise
	 * specified
	 *
	 * @param text
	 *            - the text containing node info
	 * @param name
	 *            - the name of the 'file' used for debug purposes
	 * @param only_consider
	 *            - if not null, only the specified node will be considered for
	 *            loading;all else will be ignored.
	 */
	public void loadString(String text, String name, String only_consider) {
		loadString(text, name, false, false, only_consider);
	}

	/**
	 * load all nodes contained in the text to the dialogue unless otherwise
	 * specified
	 *
	 * @param text
	 *            - the text containing node info
	 * @param name
	 *            - the name of the 'file' used for debug purposes
	 */
	public void loadString(String text, String name) {
		loadString(text, name, null);
	}

	/**
	 *
	 * @param path
	 *            - path to the file to load
	 * @param show_tokens
	 *            - if true will show the tokens generated by the lexer
	 * @param show_tree
	 *            - if true will show a tree structure generated by the parser
	 * @param only_consider
	 *            - if not null, only the specified node will be considered for
	 *            loading;all else will be ignored.
	 */
	public void loadFile(Path path, boolean show_tokens, boolean show_tree, String only_consider) throws IOException {
//		String input = Gdx.files.internal(file).readString();
			String input = new String(Files.readAllBytes(path));
			loadString(input, path.toString(), show_tokens, show_tree, only_consider);
	}


//	public void loadFile(String file, boolean show_tokens, boolean show_tree, String only_consider) {
//		String input=null;
//		try {
//			input = new String(Files.readAllBytes(Paths.get(file)));
//		} catch (IOException e) {
//			e.printStackTrace();
//			System.exit(1);
//		}
//
//		loadString(input, file, show_tokens, show_tree, only_consider);
//
//	}

	/**
	 *
	 * @param path
	 *            - path to the file to load
	 * @param only_consider
	 *            - if not null, only the specified node will be considered for
	 *            loading;all else will be ignored.
	 */
	public void loadFile(Path path, String only_consider) throws IOException {
		loadFile(path, false, false, only_consider);
	}

	/**
	 * @param path
	 *            - path to the file to load
	 *
	 */
	public void loadFile(Path path) throws IOException {
		loadFile(path, null);
	}

	// /**
	// * Start a thread that spits out results waits for results to be consumed
	// */
	// private ArrayList<RunnerResult> results = new ArrayList<Dialogue.RunnerResult>();

	private RunnerResult next_result;

	public LineHandler getLineHandler() {
		return line_handler;
	}

	public void setLineHandler(LineHandler line_handler) {
		this.line_handler = line_handler;
	}

	public OptionsHandler getOptionsHandler() {
		return option_handler;
	}

	public void setOptionsHandler(OptionsHandler options_handler) {
		this.option_handler = options_handler;
	}

	public CommandHandler getCommandHandler() {
		return command_handler;
	}

	public void setCommandHandler(CommandHandler command_handler) {
		this.command_handler = command_handler;
	}

	public NodeCompleteHandler getCompleteHandler() {
		return node_complte_handler;
	}

	public void setCompleteHandler(NodeCompleteHandler node_complete_handler) {
		this.node_complte_handler = node_complete_handler;
	}

	public boolean setNode(String name) {
	    if (vm != null) {
	    	return vm.setNode(name);
		} else {
	    	return false;
		}
    }

	public boolean start(String start) {
		next_result = null;
		execution_complete = false;

		if (debug_logger == null) {
			throw new YarnRuntimeException("debug_logger must be set before running");
		}

		if (error_logger == null) {
			throw new YarnRuntimeException("error_logger must be set before running");
		}

		if (program == null) {
			error_logger.log("Dialogue.run was called but no program was loaded.");
			return false;
		}

		vm = new VirtualMachine(this, program);

		vm.setLineHandler(line -> {
			next_result = line;
			if(line_handler != null) line_handler.handle(line);
		});

		vm.setCommandHandler(command -> {
			// if stop
			if (command.command.getCommand().equals("stop")) {
				vm.stop();
			} else if (command.getCommand().equals(VirtualMachine.EXEC_COMPLETE)) {
				execution_complete = true;
			} else {
				next_result = command;
			}
			if(command_handler != null) command_handler.handle(command);
		});

		vm.setCompleteHandler(complete -> {
			if (vm.currentNodeName() != null) {
				int count = 0;
				if (visited_node_count.containsKey(vm.currentNodeName()))
					count = visited_node_count.get(vm.currentNodeName());

				visited_node_count.put(vm.currentNodeName(), count + 1);
			}
			next_result = complete;
			if(node_complte_handler != null) node_complte_handler.handle(complete);
		});

		vm.setOptionsHandler(options -> {
			next_result = options;

			if(option_handler != null) option_handler.handle(options);
		});

		if (!vm.setNode(start)) {
			return false;
		}

		return true;
	}

	public boolean start() {
		return start(DEFAULT_START);
	}

	/**
	 * update the virtual machine counter.
	 *
	 */
	 public boolean update() {
		if (vm != null && !execution_complete && vm.getExecutionState() != ExecutionState.WaitingOnOptionSelection) {
			vm.runNext();

			return true;
		}else if(vm.getExecutionState() == ExecutionState.WaitingOnOptionSelection) {

			 System.out.println("hey");
		}
		return false;
	}

	/**
	 * get the next result - if it is null it will attempt to populate it
	 *
	 * @return
	 */
	public RunnerResult getNext() {
		// TODO: remove?
		if (vm == null) // we are not running so return null
			return null;
		checkNext();// make sure there is a next result
		RunnerResult r = next_result;
		next_result = null;
		return r;
	}

	/**
	 * get the next result - will return null if there is no result
	 *
	 * @return
	 */
	public RunnerResult nextRaw() {
		return next_result;
	}

	/**
	 * checks the next result - if it is null it will attempt to populate it
	 *
	 * @return
	 */
	public RunnerResult checkNext() {
		if (next_result == null)
			populateNext();
		return next_result;
	}

	/**
	 * check if the next result is an options result
	 *
	 * @return
	 */
	public boolean isNextOptions() {
		return checkNext() instanceof OptionResult;
	}

	/**
	 * check if the next result is a line result
	 *
	 * @return
	 */
	public boolean isNextLine() {
		return checkNext() instanceof LineResult;
	}

	/**
	 * check if the next result is a custom command result
	 *
	 * @return
	 */
	public boolean isNextCommand() {
		return checkNext() instanceof CommandResult;
	}

	/**
	 * check if the next result is a node complete result
	 *
	 * @return
	 */
	public boolean isNextComplete() {
		return checkNext() instanceof NodeCompleteResult;
	}

	/**
	 * get the next result as a line if the next result is not a line it will return
	 * null
	 *
	 * @return
	 */
	public LineResult getNextAsLine() {
		return isNextLine() ? (LineResult) getNext() : null;
	}

	/**
	 * get the next result as an options result if the next result is not an options
	 * result then this will return null
	 *
	 * @return
	 */
	public OptionResult getNextAsOptions() {
		return isNextOptions() ? (OptionResult) getNext() : null;
	}

	/**
	 * get the next result as a command result (must be parsed by the programmer) if
	 * the next result is not a commandreslt then this will return null
	 *
	 * @return
	 */
	public CommandResult getNextAsCommand() {
		return isNextCommand() ? (CommandResult) getNext() : null;
	}

	/**
	 * get the next result as a node complete result if the next result is not a
	 * node compelte result then this will return null
	 *
	 * @return
	 */
	public NodeCompleteResult getNextAsComplete() {
		return isNextComplete() ? (NodeCompleteResult) getNext() : null;
	}

	// we update the vm until the next result is no longer null
	private void populateNext() {
		while (next_result == null)
			if (!update())
				break;

	}

	// CHECK FUNCS
	// public boolean hasNext() {
	// return checkNext(0) != null;
	// }
	//
	// public boolean optionsAvailable() {
	// return checkNext(1) != null && checkNext(1) instanceof OptionResult;
	// }
	//
	// public boolean isLine() {
	// return checkNext() instanceof LineResult;
	// }
	//
	// public boolean isCommand() {
	// return checkNext() instanceof CommandResult;
	// }
	//
	// public boolean isOptions() {
	// return checkNext() instanceof OptionResult;
	// }
	//
	// public boolean isNodeComplete() {
	// return checkNext() instanceof NodeCompleteResult;
	// }
	//
	// /**
	// * offset from the end of the results stack
	// *
	// * @param offset
	// * @return
	// */
	// public RunnerResult checkNext(int offset) {
	// return results.size - Math.abs(offset) <= 0 ? null : results.get(results.size
	// - 1 - Math.abs(offset));
	// }
	//
	// public RunnerResult checkNext() {
	// return checkNext(0);
	// }
	//
	// public <t> t checkNext(Class<t> type) {
	// return type.cast(checkNext(0));
	// }
	//
	// // RETURN FUNCS
	//
	// public RunnerResult getNext() {
	// return results.size == 0 ? null : results.pop();
	// }
	//
	// public <t> t getNext(Class<t> type) {
	// return type.cast(getNext());
	// }
	//
	// public OptionResult getOptions() {
	// return getNext(OptionResult.class);
	// }
	//
	// public LineResult getLine() {
	// return getNext(LineResult.class);
	// }
	//
	// public CommandResult getCommand() {
	// return getNext(CommandResult.class);
	// }
	//
	// public NodeCompleteResult getNodeComplete() {
	// return getNext(NodeCompleteResult.class);
	// }

	public void stop() {
		if (vm != null) vm.stop();
	}

	public Set<String> allNodes() {
		return program.nodes.keySet();
	}

	public String currentNode() {
		return vm == null ? null : vm.currentNodeName();
	}

	HashMap<String, String> _tx4n;

	protected Map<String, Program.Node> getAllNodes() {
		return program.nodes;
	}

	public HashMap<String, String> getTextForAllNodes() {
		if (_tx4n == null)
			_tx4n = new HashMap<String, String>();
		_tx4n.clear();
		for (Map.Entry<String, Program.Node> entry : program.nodes.entrySet()) {
			String text = program.getTextForNode(entry.getKey());

			if (text == null)
				continue;

			_tx4n.put(entry.getKey(), text);
		}

		return _tx4n;
	}

	/**
	 * get the source code for the node
	 *
	 * @param node
	 * @return
	 */
	public String getTextForNode(String node) {
		if (program.nodes.size() == 0) {
			error_logger.log("no nodes are loaded!");
			return null;
		} else if (program.nodes.containsKey(node)) {
			return program.getTextForNode(node);
		} else {
			error_logger.log("no node named " + node);
			return null;
		}
	}

//	public void addStringTable(HashMap<String, String> string_table) {
//		program.loadStrings(string_table);
//	}

	public HashMap<String, String> getStringTable() {
		return program.strings;
	}

//	protected HashMap<String, LineInfo> getStringInfoTable() {
//		return program.line_info;
//	}

	/**
	 * unload all nodes
	 *
	 * @param clear_visisted_nodes
	 */
	public void unloadAll(boolean clear_visisted_nodes) {
		if (clear_visisted_nodes)
			visited_node_count.clear();
		program = null;
	}

	public String getByteCode() {
		return program.dumpCode(library);
	}

	public boolean nodeExists(String node_name) {
		if (program == null) {
			error_logger.log("no nodes compiled");
			return false;
		}
		if (program.nodes.size() == 0) {
			error_logger.log("no nodes in program");
			return false;
		}

		return program.nodes.containsKey(node_name);

	}

	protected void printState() {
		if (!isRunning())
			return;
		System.out.println("Current VM State:" + vm.getExecutionState().name());
	}

	public void analyse(Context context) {
		context.addProgramToAnalysis(program);
	}

	public Set<String> getvisitedNodes() {
		return visited_node_count.keySet();
	}

	public void setVisitedNodes(ArrayList<String> visited) {
		visited_node_count.clear();
		for (String string : visited) {
			visited_node_count.put(string, 1);
		}
	}

	/**
	 * unload all nodes clears visited nodes
	 */
	public void unloadAll() {
		unloadAll(true);
	}

	/**
	 * A function exposed to yarn that returns the number of times a node has been
	 * run. if no parameters are supplied, returns the number of times the current
	 * node has been run.
	 */
	protected ReturningFunc yarnFunctionNodeVisitCount = new ReturningFunc() {
		@Override
		public Object invoke(Value... params) {

			// determin ethe node were checking
			String node_name;

			if (params.length == 0) {
				// no marams? check the current node
				node_name = vm.currentNodeName();
			} else if (params.length == 1) {
				// a parameter? check the named node
				node_name = params[0].asString();
				// ensure node existance
				if (!nodeExists(node_name)) {
					String error = String.format(" the node %s does not exist.", node_name);
					error_logger.log(error);
					return 0;
				}
			} else {
				// we go ttoo many parameters
				String error = String.format("incorrect number of parameters visitcount expect 0 or 1, got %s",
						params.length);
				error_logger.log(error);
				return 0;
			}
			int visit_count = 0;
			if (visited_node_count.containsKey(node_name))
				visit_count = visited_node_count.get(node_name);
			return visit_count;
		}
	};

	protected ReturningFunc yarnFunctionIsNodeVisited = new ReturningFunc() {
		@Override
		public Object invoke(Value... params) {
			boolean visited = (Integer) yarnFunctionNodeVisitCount.invoke(params) > 0;
			return visited;
		}
	};

	protected Program.Node getCurrentNode() {
		return (vm != null) ? vm.getCurrentNode() : null;
	}

	// ======================================================================================

//	/**
//	 * indicates something the client should do
//	 */
//	public static abstract class RunnerResult {
//		// private boolean consumed = false;
//		// public void consume() {consumed = true;}
//		// private boolean isConsumed() {return consumed;}
//	}

//	/**
//	 * the client should run a line of dialogue
//	 */
//	public static class LineResult extends RunnerResult {
//		protected Line line;
//
//		public LineResult(String text) {
//			line = new Line(text);
//		}
//
//		public String getText() {
//			return line.;
//		}
//	}

//	/**
//	 * client should run and parse command
//	 */
//	public static class CommandResult extends RunnerResult {
//		protected Command command;
//
//		public CommandResult(String text) {
//			command = new Command(text);
//		}
//
//		public String getCommand() {
//			return command.command;
//		}
//
//	}

//	/**
//	 * Client should show a list of options and call the chooser choose before
//	 * asking for the next line.
//	 */
//	public static class OptionResult extends RunnerResult {
//		protected Options options;
//		protected OptionChooser chooser;
//
//		public OptionResult(ArrayList<String> options, OptionChooser chooser) {
//			this.chooser = chooser;
//			this.options = new Options(options);
//		}
//
//		public ArrayList<String> getOptions() {
//			return options.getOptions();
//		}
//
//		public void choose(int choice) {
//			chooser.choose(choice);
//		}
//	}

//	/**
//	 * end of node reached
//	 */
//	public static class NodeCompleteResult extends RunnerResult {
//		public String next_node;
//
//		public NodeCompleteResult(String next_node) {
//			this.next_node = next_node;
//		}
//	}

	/**
	 * something went wrong
	 *
	 */
	public static class YarnRuntimeException extends RuntimeException {
		private static final long serialVersionUID = -5732778106783039900L;

		public YarnRuntimeException(String message) {
			super(message);
		}

		public YarnRuntimeException(Throwable t) {
			super(t);
		}

		public YarnRuntimeException(String message, Throwable t) {
			super(message, t);
		}

	}

	/**
	 * option chooser lets client tell dialogue the response selected by the user
	 */
	public static interface OptionChooser {
		public void choose(int selected_option_index);
	}

	/**
	 * logger to let the client send output to the console logging/error logging
	 */
	public static interface YarnLogger {
		public void log(String message);
	}

	/**
	 *
	 *
	 * information that the client should handle
	 */
	public static class Line {
		public String id;
		public String[] substitutions;

		public Line(String id) {
			this.id = id;
			substitutions = new String[0];
		}
	}

	
	public static class OptionSet{
		private Option[] options;
		public OptionSet(Option...options) {
			this.options = options;
		}
		
		public Option[] getOptions() {
			return options;
		}
	}
	
	public static class Option{
		private Line line;
		private int id;
		private String destination;
		
		public Option(Line line,int id,String destination) {
			this.setLine(line);
			this.setId(id);
			this.setDestination(destination);
		}

		public Line getLine() {
			return line;
		}

		private void setLine(Line line) {
			this.line = line;
		}

		public int getId() {
			return id;
		}

		private void setId(int id) {
			this.id = id;
		}

		public String getDestination() {
			return destination;
		}

		private void setDestination(String destination) {
			this.destination = destination;
		}
		
		
		
	}
	
//	public static class Options {
//		private ArrayList<String> options;
//
//		public Options(ArrayList<String> options) {
//			this.options = options;
//		}
//
//		public ArrayList<String> getOptions() {
//			return options;
//		}
//
//		public void setOptions(ArrayList<String> options) {
//			this.options = options;
//		}
//	}

	public static class Command {
		private String command;

		public Command(String command) {
			this.command = command;
		}

		public String getCommand() {
			return command;
		}
	}

	/**
	 * variable storage TODO: try to use {@link com.kyper.yarn.DialogueData UserData}
	 */
	public static interface VariableStorage {
		public void setValue(String name, Value value);

		public Value getValue(String name);

		public void clear();
	}

	public static abstract class BaseVariableStorage implements VariableStorage {

	}

	public static class MemoryVariableStorage extends BaseVariableStorage {

		HashMap<String, Value> variables = new HashMap<String, Value>();

		@Override
		public void setValue(String name, Value value) {
			variables.put(name, value);
		}

		@Override
		public Value getValue(String name) {
			Value value = Value.NULL;
			if (variables.containsKey(name))
				value = variables.get(name);
			return value;
		}

		@Override
		public void clear() {
			variables.clear();
		}

	}

	/**
	 * a line localized into the current locale that is used in lines, options and
	 * shortcut options. Anything that is user-facing.
	 */
//	public static class LocalisedLine {
//		private String code;
//		private String text;
//		private String comment;
//
//		public LocalisedLine(String code, String text, String comment) {
//			this.code = code;
//			this.text = text;
//			this.comment = comment;
//		}
//
//		public String getCode() {
//			return code;
//		}
//
//		public String getText() {
//			return text;
//		}
//
//		public String getComment() {
//			return comment;
//		}
//
//		public void setCode(String code) {
//			this.code = code;
//		}
//
//		public void setText(String text) {
//			this.text = text;
//		}
//
//		public void setComment(String comment) {
//			this.comment = comment;
//		}
//	}

	/**
	 * the standrad built in lib of functions and operators
	 */
	private static class StandardLibrary extends Library {

		public StandardLibrary() {
			// operations

			registerFunction(TokenType.Add.name(), 2, new ReturningFunc() {
				@Override
				public Object invoke(Value... params) {
					return params[0].add(params[1]);
				}
			});

			registerFunction(TokenType.Minus.name(), 2, new ReturningFunc() {
				@Override
				public Object invoke(Value... params) {
					return params[0].sub(params[1]);
				}
			});

			registerFunction(TokenType.UnaryMinus.name(), 1, new ReturningFunc() {
				@Override
				public Object invoke(Value... params) {
					return params[0].negative();
				}
			});

			registerFunction(TokenType.Divide.name(), 2, new ReturningFunc() {
				@Override
				public Object invoke(Value... params) {
					return params[0].div(params[1]);
				}
			});

			registerFunction(TokenType.Multiply.name(), 2, new ReturningFunc() {
				@Override
				public Object invoke(Value... params) {
					return params[0].mul(params[1]);
				}
			});

			registerFunction(TokenType.Modulo.name(), 2, new ReturningFunc() {
				@Override
				public Object invoke(Value... params) {
					return params[0].mod(params[1]);
				}
			});

			registerFunction(TokenType.EqualTo.name(), 2, new ReturningFunc() {
				@Override
				public Object invoke(Value... params) {
					return params[0].equals(params[1]);
				}
			});

			registerFunction(TokenType.NotEqualTo.name(), 2, new ReturningFunc() {
				@Override
				public Object invoke(Value... params) {
					return !params[0].equals(params[1]);
				}
			});

			registerFunction(TokenType.GreaterThan.name(), 2, new ReturningFunc() {
				@Override
				public Object invoke(Value... params) {
					return params[0].greaterThan(params[1]);
				}
			});

			registerFunction(TokenType.GreaterThanOrEqualTo.name(), 2, new ReturningFunc() {
				@Override
				public Object invoke(Value... params) {
					return params[0].greaterThanOrEqual(params[1]);
				}
			});

			registerFunction(TokenType.LessThan.name(), 2, new ReturningFunc() {
				@Override
				public Object invoke(Value... params) {
					return params[0].lessThan(params[1]);
				}
			});

			registerFunction(TokenType.LessThanOrEqualTo.name(), 2, new ReturningFunc() {
				@Override
				public Object invoke(Value... params) {
					return params[0].lessThanOrEqual(params[1]);
				}
			});

			registerFunction(TokenType.And.name(), 2, new ReturningFunc() {
				@Override
				public Object invoke(Value... params) {
					return params[0].asBool() && params[1].asBool();
				}
			});

			registerFunction(TokenType.Or.name(), 2, new ReturningFunc() {
				@Override
				public Object invoke(Value... params) {
					return params[0].asBool() || params[1].asBool();
				}
			});

			registerFunction(TokenType.Xor.name(), 2, new ReturningFunc() {
				@Override
				public Object invoke(Value... params) {
					return params[0].asBool() ^ params[1].asBool();
				}
			});

			registerFunction(TokenType.Not.name(), 1, new ReturningFunc() {
				@Override
				public Object invoke(Value... params) {
					return !params[0].asBool();
				}
			});

			// end operations ===

		}
	}

}
