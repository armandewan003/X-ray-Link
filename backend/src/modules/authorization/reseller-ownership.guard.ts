import { CanActivate, ExecutionContext, ForbiddenException, Injectable } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';

@Injectable()
export class ResellerOwnershipGuard implements CanActivate {
  constructor(private readonly prisma: PrismaService) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const req = context.switchToHttp().getRequest();
    const principal = req.user;
    if (!principal) throw new ForbiddenException('Unauthenticated');
    if (principal.role === 'OWNER' || principal.role === 'ADMIN') return true;
    if (principal.role !== 'RESELLER') throw new ForbiddenException('Forbidden');

    const targetUserId = req.params?.userId || req.params?.id;
    if (!targetUserId) return true;

    const assignment = await this.prisma.resellerUserAssignment.findUnique({
      where: { userId: targetUserId },
    });
    if (!assignment || assignment.resellerId !== principal.sub) {
      throw new ForbiddenException('Cross-reseller access prohibited');
    }
    return true;
  }
}
