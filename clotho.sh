#!/bin/bash
# ------------------------------------------------------------------
# [Kia Rahmani] CLOTHO
#          	Directed Test Generation for Weakly Consistent Database Systems 
# ------------------------------------------------------------------
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
# INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
# PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
# ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
# ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
# ------------------------------------------------------------------



VERSION=1.0.0
USAGE="Usage: command -ihv args"

# --- Options processing -------------------------------------------
if [ $# == 0 ] ; then
    echo $USAGE
    exit 1;
fi




# FUNCTIONS
# -----------------------------------------------------------------
visualize () {
  echo ">> visualizing anomaly #${ANML_NO} from ${BENCHMARK}"
}
# -----------------------------------------------------------------
analyze () {
  echo ">> analyzing benchmark ${BENCHMARK}"
}
# -----------------------------------------------------------------
client () {
  echo "running a client"
}
# -----------------------------------------------------------------
drive () {
  echo "running the scheduler"
}
# -----------------------------------------------------------------
setup () {
  echo "setting up the clusters and intializing them"
}





# BODY 
# -----------------------------------------------------------------

BENCHMARK=$2

while [[ $# -gt 0 ]]
do
KEY="$1"

case $KEY in
    -v|--version)
    echo $VERSION
    shift # past argument
    ;;
    -a|--analyze)
    analyze
    shift # past argument
    shift # past value
    ;;
    -d|--drive)
    ANML_NO=$3
    DELAY=$4
    drive
    shift # past argument
    shift # past value
    shift # past value
    shift # past value
    ;;
    -c|--client)
    ANML_NO=$3
    CLIENT_NO=$4
    client
    shift # past argument
    shift # past value
    ;;
    -s|--setup)
    setup
    shift # past argument
    shift # past value
    ;;
    -z|--visualize)
    ANML_NO=$3
    visualize
    shift # past argument
    shift # past value
    shift # past value
    ;;
    --default)
    DEFAULT=YES
    shift # past argument
    ;;
    *)    # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    echo $USAGE
    shift # past argument
    shift # past argument
    shift # past argument
    ;;
esac
done


































