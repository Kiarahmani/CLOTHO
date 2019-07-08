# CLOTHO

git clone https://github.com/Kiarahmani/CLOTHO.git
cd CLOTHO
./clotho.sh --setup 2
./clotho.sh --cluster
make benchmark=dirty_write
./clotho.sh --analyze dirty_write 

./clotho.sh --show dirty_write 1
./clotho.sh --init dirty_write 1
./clotho.sh -d dirty_write 1

./clotho.sh --client dirty_write 1 1

