#include <mpi.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#define NRA 62   /* rows of matrix A */
#define NCA 15   /* columns of matrix A */
#define NCB 7    /* columns of matrix B */
#define MASTER 0
#define FROM_MASTER 1
#define FROM_WORKER 2

int main(int argc, char *argv[]) {
    int numtasks, taskid, numworkers, source, dest, rows;
    int averow, extra, offset, i, j, k;
    double a[NRA][NCA], b[NCA][NCB], c[NRA][NCB];
    MPI_Status status;
    double start_time, end_time;

    MPI_Init(&argc, &argv);
    MPI_Comm_size(MPI_COMM_WORLD, &numtasks);
    MPI_Comm_rank(MPI_COMM_WORLD, &taskid);

    if (numtasks < 2) {
        printf("You need at least 2 MPI tasks (1 master + >=1 worker). Exiting...\n");
        MPI_Abort(MPI_COMM_WORLD, 1);
        exit(1);
    }

    numworkers = numtasks - 1;

    if (taskid == MASTER) {
        printf("Running with %d tasks (1 master + %d workers)\n", numtasks, numworkers);

        /* Initialize matrices */
        for (i = 0; i < NRA; i++)
            for (j = 0; j < NCA; j++)
                a[i][j] = 10;

        for (i = 0; i < NCA; i++)
            for (j = 0; j < NCB; j++)
                b[i][j] = 10;

        /* Distribute work */
        averow = NRA / numworkers;
        extra = NRA % numworkers;
        offset = 0;

        start_time = MPI_Wtime();

        for (dest = 1; dest <= numworkers; dest++) {
            rows = (dest <= extra) ? averow + 1 : averow;

            MPI_Send(&offset, 1, MPI_INT, dest, FROM_MASTER, MPI_COMM_WORLD);
            MPI_Send(&rows, 1, MPI_INT, dest, FROM_MASTER, MPI_COMM_WORLD);
            MPI_Send(&a[offset][0], rows * NCA, MPI_DOUBLE, dest, FROM_MASTER, MPI_COMM_WORLD);
            MPI_Send(&b, NCA * NCB, MPI_DOUBLE, dest, FROM_MASTER, MPI_COMM_WORLD);

            offset += rows;
        }

        /* Receive results */
        for (source = 1; source <= numworkers; source++) {
            MPI_Recv(&offset, 1, MPI_INT, source, FROM_WORKER, MPI_COMM_WORLD, &status);
            MPI_Recv(&rows, 1, MPI_INT, source, FROM_WORKER, MPI_COMM_WORLD, &status);
            MPI_Recv(&c[offset][0], rows * NCB, MPI_DOUBLE, source, FROM_WORKER, MPI_COMM_WORLD, &status);
        }

        end_time = MPI_Wtime();

        /* Display result */
        printf("Execution time (Blocking): %f seconds\n", end_time - start_time);
        printf("Result Matrix:\n");
        for (i = 0; i < NRA; i++) {
            for (j = 0; j < NCB; j++)
                printf("%6.2f ", c[i][j]);
            printf("\n");
        }

        printf("Calculation completed.\n");

    } else {
        /* Worker */
        MPI_Recv(&offset, 1, MPI_INT, MASTER, FROM_MASTER, MPI_COMM_WORLD, &status);
        MPI_Recv(&rows, 1, MPI_INT, MASTER, FROM_MASTER, MPI_COMM_WORLD, &status);
        MPI_Recv(&a, rows * NCA, MPI_DOUBLE, MASTER, FROM_MASTER, MPI_COMM_WORLD, &status);
        MPI_Recv(&b, NCA * NCB, MPI_DOUBLE, MASTER, FROM_MASTER, MPI_COMM_WORLD, &status);

        for (k = 0; k < NCB; k++)
            for (i = 0; i < rows; i++) {
                c[i][k] = 0.0;
                for (j = 0; j < NCA; j++)
                    c[i][k] += a[i][j] * b[j][k];
            }

        MPI_Send(&offset, 1, MPI_INT, MASTER, FROM_WORKER, MPI_COMM_WORLD);
        MPI_Send(&rows, 1, MPI_INT, MASTER, FROM_WORKER, MPI_COMM_WORLD);
        MPI_Send(&c, rows * NCB, MPI_DOUBLE, MASTER, FROM_WORKER, MPI_COMM_WORLD);
    }

    MPI_Finalize();
    return 0;
}
